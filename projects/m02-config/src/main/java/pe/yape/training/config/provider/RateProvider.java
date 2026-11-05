package pe.yape.training.config.provider;

import java.math.BigDecimal;
import java.util.Optional;

/** Puerto: de dónde obtenemos la tasa PEN -> moneda. */
public interface RateProvider {
    Optional<BigDecimal> rateFor(String currency);
}
