package pe.yape.training.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

/**
 * Reto 2: el circuito abre tras 4 fallos (requestVolumeThreshold=4, failureRatio=1.0),
 * permanece abierto 5 s y luego pasa a half-open.
 *
 * Importante: CustomerGateway es @ApplicationScoped (singleton), así que el estado del
 * circuito persiste entre métodos @Test de la misma instancia de la app. Por eso todo el
 * ciclo (abrir -> fallback sin llamar al cliente -> esperar -> recuperar) se prueba en UN
 * solo test, en orden, para no depender del orden de ejecución de JUnit.
 */
@QuarkusTest
class CustomerGatewayCircuitBreakerTest {

    @InjectMock
    @RestClient
    CustomerClient customerClient;

    @Inject
    CustomerGateway gateway;

    @Test
    void circuitoAbreSinLlamarAlClienteYLuegoSeRecuperaEnHalfOpen() {
        // --- 1) El cliente real siempre falla ---
        when(customerClient.get(anyLong(), anyLong()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("boom")));

        // 1ra llamada a find(): @Retry agota sus 2 reintentos => 3 invocaciones reales al
        // cliente, todas fallan. El circuito todavía no decide nada: va 3 de 4 en su ventana.
        Customer primero = gateway.find(1L, 0).await().indefinitely();
        assertEquals("Desconocido", primero.name());
        verify(customerClient, times(3)).get(anyLong(), anyLong());

        // 2da llamada a find(): su primer intento es el 4to fallo real => el circuito ABRE
        // en ese instante. Los reintentos restantes de esta misma llamada ya no tocan el
        // cliente real (reciben CircuitBreakerOpenException de inmediato).
        Customer segundo = gateway.find(1L, 0).await().indefinitely();
        assertEquals("Desconocido", segundo.name());
        verify(customerClient, times(4)).get(anyLong(), anyLong());

        // --- 2) Con el circuito ABIERTO, una 3ra llamada NO debe tocar el cliente real ---
        Customer tercero = gateway.find(1L, 0).await().indefinitely();
        assertEquals("Desconocido", tercero.name());
        verify(customerClient, times(4)).get(anyLong(), anyLong()); // sigue en 4: ninguna llamada nueva

        // --- 3) Esperamos los 5 s configurados en @CircuitBreaker(delay = 5000) ---
        sleepUninterruptibly(5100);

        // --- 4) El servicio "vuelve a responder": medio abierto, una llamada de prueba pasa ---
        when(customerClient.get(anyLong(), anyLong()))
                .thenReturn(Uni.createFrom().item(new Customer(1L, "Ana Torres", "GOLD")));

        Customer recuperado = gateway.find(1L, 0).await().indefinitely();
        assertEquals("Ana Torres", recuperado.name());
        assertFalse("N/A".equals(recuperado.tier()));
        // esta sí llamó al cliente real: pasamos de 4 a 5 invocaciones totales.
        verify(customerClient, times(5)).get(anyLong(), anyLong());

        // --- 5) Con successThreshold=1, una sola respuesta buena ya cerró el circuito: ---
        Customer confirmacion = gateway.find(1L, 0).await().indefinitely();
        assertEquals("Ana Torres", confirmacion.name());
        verify(customerClient, times(6)).get(anyLong(), anyLong());
    }

    private static void sleepUninterruptibly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
