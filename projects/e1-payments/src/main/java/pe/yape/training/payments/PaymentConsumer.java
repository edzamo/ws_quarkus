package pe.yape.training.payments;

import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletionStage;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

@ApplicationScoped
public class PaymentConsumer {

    static final BigDecimal LIMITE = new BigDecimal("10000");
    private static final Logger LOG = Logger.getLogger(PaymentConsumer.class);

    // En producción esto sería una tabla (clave única) o Redis; en memoria solo para aprender.
    private final Set<String> processed = ConcurrentHashMap.newKeySet();

    /**
     * Kafka garantiza "al menos una vez": el mismo mensaje puede llegar repetido.
     * Por eso el consumidor debe ser IDEMPOTENTE (procesar dos veces = procesar una).
     */
    @Incoming("payments-in")
    public CompletionStage<Void> consume(Message<PagoRealizado> msg) {
        PagoRealizado p = msg.getPayload();
        if (p.monto().compareTo(LIMITE) > 0) {
            // nack -> failure-strategy dead-letter-queue -> topic payments-dlq
            LOG.warnf("Pago %s rechazado (monto %s > límite)", p.id(), p.monto());
            return msg.nack(new IllegalArgumentException("Monto sobre el límite: " + p.monto()));
        }
        if (!processed.add(p.id())) {
            LOG.infof("Pago %s duplicado: se ignora", p.id());
        } else {
            LOG.infof("Pago %s procesado: %s -> %s %s", p.id(), p.origen(), p.destino(), p.monto());
        }
        return msg.ack();
    }

    public List<String> processedIds() {
        return processed.stream().sorted().toList();
    }
}
