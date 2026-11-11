package pe.yape.training.payments;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import java.util.HashMap;
import java.util.Map;

/** Cambia los canales de Kafka por el conector en memoria SOLO durante los tests. */
public class InMemoryKafkaResource implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        Map<String, String> env = new HashMap<>();
        env.putAll(InMemoryConnector.switchIncomingChannelsToInMemory("payments-in"));
        env.putAll(InMemoryConnector.switchOutgoingChannelsToInMemory("payments-out"));
        return env;
    }

    @Override
    public void stop() {
        InMemoryConnector.clear();
    }
}
