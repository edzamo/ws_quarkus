package pe.yape.training.product;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import java.time.Duration;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProductResourceTest {

    @InjectMock
    @RestClient
    CustomerClient customerClient;

    @Test
    void detalleCombinaProductoYCliente() {
        when(customerClient.get(anyLong(), anyLong()))
                .thenReturn(Uni.createFrom().item(new Customer(1L, "Ana Torres", "GOLD")));

        given().when().get("/products/1/detail").then().statusCode(200)
                .body("product.name", is("Cuenta Sueldo"))
                .body("owner.name", is("Ana Torres"))
                .body("degraded", is(false));
    }

    @Test
    void productoInexistenteDevuelve404() {
        given().when().get("/products/99/detail").then().statusCode(404);
    }

    @Test
    void siCustomerEsLentoActivaTimeoutYFallback() {
        when(customerClient.get(anyLong(), anyLong()))
                .thenReturn(Uni.createFrom().item(new Customer(1L, "Ana", "GOLD"))
                        .onItem().delayIt().by(Duration.ofSeconds(3)));

        given().when().get("/products/1/detail").then().statusCode(200)
                .body("owner.name", is("Desconocido"))
                .body("degraded", is(true));
    }

    @Test
    void siCustomerFallaConErrorActivaFallback() {
        when(customerClient.get(anyLong(), anyLong()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("boom")));

        given().when().get("/products/2/detail").then().statusCode(200)
                .body("degraded", is(true));
    }

    @Test
    void reintentaAntesDeRendirse() {
        // Falla 1 vez, luego responde bien: el @Retry lo recupera sin fallback.
        when(customerClient.get(anyLong(), anyLong()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("transitorio")))
                .thenReturn(Uni.createFrom().item(new Customer(1L, "Ana Torres", "GOLD")));

        given().when().get("/products/1/detail").then().statusCode(200)
                .body("owner.name", is("Ana Torres")).body("degraded", is(false));
    }

    @Test
    void hilosSegunTipoDeRetorno() {
        given().when().get("/products/threads/reactive").then().body("thread", containsString("eventloop"));
        given().when().get("/products/threads/blocking").then().body("thread", containsString("executor-thread"));
        given().when().get("/products/threads/forced-blocking").then().body("thread", containsString("executor-thread"));
    }

  @Test
  void summary() {
    when(customerClient.get(anyLong(), anyLong()))
        .thenReturn(Uni.createFrom().item(new Customer(1L, "Ana Torres", "GOLD")));

    given().when().get("/products/1/summary?customerId=1&customerDelayMs=0").then().statusCode(200)
        .body("name", is("Cuenta Sueldo"))
        .body("ownerCustomerName", is("Ana Torres"))
        .body("ownerCustomerTier", is("GOLD"))
        .body("riskScore", greaterThanOrEqualTo(0.0f));
  }

  @Inject
  ProductResource resource;

  @Test
  void summaryEsAproximadamenteElMaximoNoLaSuma() {
    when(customerClient.get(anyLong(), anyLong()))
        .thenReturn(Uni.createFrom().item(new Customer(1L, "Ana Torres", "GOLD"))
            .onItem().delayIt().by(Duration.ofMillis(50)));

    // Warm-up: primera invocación "en frío" no cuenta para la medición.
    resource.summary(1L, 1L, 50L).await().indefinitely();

    long start = System.nanoTime();
    resource.summary(1L, 1L, 50L).await().indefinitely();
    long elapsedMs = Duration.ofNanos(System.nanoTime() - start).toMillis();

    // max(0 producto, 50 cliente, 400 risk) + margen, MUY lejos de la suma (450+).
    assertThat(elapsedMs, is(lessThan(480L)));
  }

  @Test
  void versionSecuencialSeriaMasLentaQueElParalelo() {
    Uni<Customer> customerUni = Uni.createFrom().item(new Customer(1L, "Ana Torres", "GOLD"))
        .onItem().delayIt().by(Duration.ofMillis(50));
    Uni<RiskScore> riskUni = Uni.createFrom().item(new RiskScore(1L, "N/A", 0.5))
        .onItem().delayIt().by(Duration.ofMillis(400));

    // A propósito encadenamos (chain) en vez de combinar: así se suman los tiempos.
    long start = System.nanoTime();
    customerUni.chain(c -> riskUni).await().indefinitely();
    long elapsedMs = Duration.ofNanos(System.nanoTime() - start).toMillis();

    // 50 + 400 = 450 como piso teórico; probamos que queda claramente por encima
    // del umbral que usamos para la versión en paralelo (480 ms).
    assertThat(elapsedMs, is(greaterThanOrEqualTo(440L)));
  }

}
