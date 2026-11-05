package pe.yape.training.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import pe.yape.training.config.audit.Audited;
import pe.yape.training.config.provider.RateProvider;

@ApplicationScoped
public class ExchangeService {

    private final ExchangeConfig config;
    private final RateProvider provider;
    private final Clock clock;

    // Inyección por constructor: recomendada (inmutable y testeable). Sin @Inject basta si hay un solo constructor.
    ExchangeService(ExchangeConfig config, RateProvider provider, Clock clock) {
        this.config = config;
        this.provider = provider;
        this.clock = clock;
    }

    @Audited
    public Conversion convert(String to, BigDecimal amount) {
        BigDecimal rate = provider.rateFor(to)
                .orElseThrow(() -> new BadRequestException("Moneda no soportada: " + to));
        BigDecimal effectiveRate = rate.multiply(BigDecimal.ONE.subtract(config.spread()));
        BigDecimal result = amount.multiply(effectiveRate).setScale(2, RoundingMode.HALF_UP);
        return new Conversion(config.baseCurrency(), to.toUpperCase(), amount, rate,
                config.spread(), result, clock.instant());
    }
}
