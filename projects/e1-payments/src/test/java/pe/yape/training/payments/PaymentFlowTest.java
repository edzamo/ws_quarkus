package pe.yape.training.payments;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Duration;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.junit.jupiter.api.Test;

/**
 * Tests con conector EN MEMORIA: rápidos y sin Docker. Verifican la lógica del productor y del consumidor.
 * El DLQ real (topic payments-dlq) se comprueba a mano con Kafka en dev (ver ejercicio).
 */
@QuarkusTest
@QuarkusTestResource(InMemoryKafkaResource.class)
class PaymentFlowTest {

    @Inject @Any InMemoryConnector connector;
    @Inject PaymentConsumer consumer;

    @Test
    void elEndpointPublicaElEvento() {
        InMemorySink<PagoRealizado> sink = connector.sink("payments-out");
        sink.clear();

        given().contentType("application/json")
                .body("{\"id\":\"p-1\",\"origen\":\"999111222\",\"destino\":\"999333444\",\"monto\":50}")
                .when().post("/payments").then().statusCode(202);

        await().atMost(Duration.ofSeconds(5)).until(() -> sink.received().size() == 1);
        assertEquals("p-1", sink.received().get(0).getPayload().id());
    }

    @Test
    void elConsumidorEsIdempotente() {
        InMemorySource<PagoRealizado> source = connector.source("payments-in");
        PagoRealizado pago = new PagoRealizado("dup-1", "a", "b", new BigDecimal("10"));

        source.send(pago);
        source.send(pago); // llega repetido

        await().atMost(Duration.ofSeconds(5)).until(() -> consumer.processedIds().contains("dup-1"));
        assertEquals(1, consumer.processedIds().stream().filter("dup-1"::equals).count());
    }

    @Test
    void montoSobreElLimiteNoSeProcesa() throws Exception {
        InMemorySource<PagoRealizado> source = connector.source("payments-in");
        source.send(new PagoRealizado("big-1", "a", "b", new BigDecimal("20000")));
        Thread.sleep(500);
        assertEquals(false, consumer.processedIds().contains("big-1"));
    }
}
