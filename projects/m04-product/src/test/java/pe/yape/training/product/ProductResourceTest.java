package pe.yape.training.product;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
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
}
