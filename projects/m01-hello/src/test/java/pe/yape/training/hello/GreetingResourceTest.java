package pe.yape.training.hello;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class GreetingResourceTest {

    @Test
    void helloConNombrePorDefecto() {
        given().when().get("/hello")
                .then().statusCode(200)
                .body(is("Hola mundo, bienvenido a Quarkus en Yape!"));
    }

    @Test
    void helloConNombre() {
        given().queryParam("name", "Yape").when().get("/hello")
                .then().statusCode(200)
                .body(is("Hola Yape, bienvenido a Quarkus en Yape!"));
    }

    @Test
    void healthEstaUp() {
        given().when().get("/q/health")
                .then().statusCode(200).body("status", is("UP"));
    }
}
