package pe.yape.training.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.time.Clock;
import java.time.ZoneId;

/** Producer CDI (≈ @Bean de Spring @Configuration). Permite inyectar y mockear el reloj. */
@ApplicationScoped
public class ClockProducer {

    @Produces
    @ApplicationScoped
    Clock clock() {
        return Clock.system(ZoneId.of("America/Lima"));
    }
}
