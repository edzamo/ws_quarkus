package pe.yape.training.config;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** @TestProfile permite sobreescribir config SOLO para esta clase de test. */
@QuarkusTest
@TestProfile(ExchangeNoSpreadTest.NoSpread.class)
class ExchangeNoSpreadTest {

    public static class NoSpread implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("exchange.spread", "0");
        }
    }

    @Test
    void sinSpreadElResultadoEsLaTasaPura() {
        given().queryParam("to", "USD").queryParam("amount", "100")
                .when().get("/exchange/convert")
                .then().statusCode(200).body("result", is(27.0f));
    }
}
