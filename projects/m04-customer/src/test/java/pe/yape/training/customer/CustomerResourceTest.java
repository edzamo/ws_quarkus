package pe.yape.training.customer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CustomerResourceTest {

    @Test
    void obtienePorId() {
        given().when().get("/customers/1").then().statusCode(200)
                .body("name", is("Ana Torres")).body("tier", is("GOLD"));
    }

    @Test
    void noExistenteDevuelve404() {
        given().when().get("/customers/99").then().statusCode(404);
    }

    @Test
    void listaEmiteTodos() {
        given().when().get("/customers").then().statusCode(200).body("size()", is(3));
    }

    @Test
    void respetaElRetardoSimulado() {
        long start = System.currentTimeMillis();
        given().queryParam("delayMs", 300).when().get("/customers/1").then().statusCode(200);
        org.junit.jupiter.api.Assertions.assertTrue(System.currentTimeMillis() - start >= 300);
    }
}
