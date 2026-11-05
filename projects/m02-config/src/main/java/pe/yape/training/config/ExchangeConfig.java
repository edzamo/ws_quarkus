package pe.yape.training.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Config tipada (equivale a @ConfigurationProperties de Spring).
 * Se valida al ARRANCAR: si falta "exchange.base-currency" el app no levanta.
 */
@ConfigMapping(prefix = "exchange")
public interface ExchangeConfig {

    String baseCurrency();

    BigDecimal spread();

    @WithDefault("config")
    String provider();

    Map<String, BigDecimal> rates();
}
