package pe.yape.training.config;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/** En perfil test spread=0.02 (base) -> 100 PEN * 0.27 * 0.98 = 26.46 */
@QuarkusTest
class ExchangeResourceTest {

    @Test
    void convierteAplicandoSpread() {
        given().queryParam("to", "usd").queryParam("amount", "100")
                .when().get("/exchange/convert")
                .then().statusCode(200)
                .body("result", is(26.46f))
                .body("from", is("PEN"))
                .body("to", is("USD"));
    }

    @Test
    void monedaNoSoportadaDevuelve400() {
        given().queryParam("to", "JPY").queryParam("amount", "100")
                .when().get("/exchange/convert")
                .then().statusCode(400);
    }

    @Test
    void montoInvalidoDevuelve400PorValidacion() {
        given().queryParam("to", "USD").queryParam("amount", "-5")
                .when().get("/exchange/convert")
                .then().statusCode(400);
    }

    @Test
    void leeConfigPropertyConDefaultDeVariableDeEntorno() {
        given().when().get("/exchange/info")
                .then().statusCode(200).body(is("m02-config desde Yape"));
    }
}
