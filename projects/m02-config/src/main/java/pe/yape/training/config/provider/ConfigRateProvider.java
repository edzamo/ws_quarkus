package pe.yape.training.config.provider;

import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.Optional;
import pe.yape.training.config.ExchangeConfig;

/**
 * Se activa si exchange.provider=config (o si falta).
 * @IfBuildProperty se decide en BUILD TIME: el bean que no aplica ni siquiera existe en el jar.
 */
@ApplicationScoped
@IfBuildProperty(name = "exchange.provider", stringValue = "config", enableIfMissing = true)
public class ConfigRateProvider implements RateProvider {

    @Inject
    ExchangeConfig config;

    @Override
    public Optional<BigDecimal> rateFor(String currency) {
        return Optional.ofNullable(config.rates().get(currency.toUpperCase()));
    }
}
