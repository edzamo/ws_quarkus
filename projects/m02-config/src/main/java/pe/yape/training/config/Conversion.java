package pe.yape.training.config;

import java.math.BigDecimal;
import java.time.Instant;

public record Conversion(String from, String to, BigDecimal amount, BigDecimal rate,
                         BigDecimal spread, BigDecimal result, Instant timestamp) {
}
