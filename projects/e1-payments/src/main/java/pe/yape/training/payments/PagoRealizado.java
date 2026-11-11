package pe.yape.training.payments;

import java.math.BigDecimal;

/** Evento de dominio (pasado: algo que YA ocurrió). El id permite idempotencia en el consumidor. */
public record PagoRealizado(String id, String origen, String destino, BigDecimal monto) {
}
