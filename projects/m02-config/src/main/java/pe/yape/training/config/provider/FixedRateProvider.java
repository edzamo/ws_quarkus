package pe.yape.training.config.provider;

import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.util.Optional;

/** Alternativa para pruebas: tasa fija 1:1. Se activa con exchange.provider=fixed. */
@ApplicationScoped
@IfBuildProperty(name = "exchange.provider", stringValue = "fixed")
public class FixedRateProvider implements RateProvider {

    @Override
    public Optional<BigDecimal> rateFor(String currency) {
        return Optional.of(BigDecimal.ONE);
    }
}
