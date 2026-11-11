package pe.yape.training.customer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/** Corre contra un PostgreSQL REAL (Dev Services) con el esquema creado por Flyway. */
@QuarkusTest
class CustomerResourceTest {

    @Test
    void migracionesCargaronLaSemilla() {
        given().when().get("/customers/1").then().statusCode(200)
                .body("name", is("Ana Torres")).body("tier", is("GOLD"));
    }

    @Test
    void noExistenteDevuelve404() {
        given().when().get("/customers/9999").then().statusCode(404);
    }

    @Test
    void creaYLuegoLee() {
        String location = given().contentType("application/json")
                .body("{\"name\":\"Marta Ruiz\"}")
                .when().post("/customers").then().statusCode(201)
                .body("tier", is("BASIC")).extract().header("Location");

        given().when().get(location).then().statusCode(200).body("name", is("Marta Ruiz"));
    }

    @Test
    void validaNombreYTier() {
        given().contentType("application/json").body("{\"name\":\" \"}")
                .when().post("/customers").then().statusCode(400);
        given().contentType("application/json").body("{\"name\":\"X\",\"tier\":\"PLATINO\"}")
                .when().post("/customers").then().statusCode(400);
    }

    @Test
    void actualizaYBorra() {
        String location = given().contentType("application/json").body("{\"name\":\"Temp\"}")
                .when().post("/customers").then().statusCode(201).extract().header("Location");

        given().contentType("application/json").body("{\"name\":\"Temp 2\",\"tier\":\"GOLD\"}")
                .when().put(location).then().statusCode(200)
                .body("name", is("Temp 2")).body("tier", is("GOLD"));

        given().when().delete(location).then().statusCode(204);
        given().when().get(location).then().statusCode(404);
        given().when().delete(location).then().statusCode(404);
    }

    @Test
    void listaOrdenadaPorId() {
        given().when().get("/customers").then().statusCode(200)
                .body("size()", greaterThanOrEqualTo(3))
                .body("[0].name", is("Ana Torres"));
    }
}
