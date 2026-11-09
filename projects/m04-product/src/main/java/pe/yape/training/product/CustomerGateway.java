package pe.yape.training.product;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.temporal.ChronoUnit;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

/**
 * Frontera con el servicio externo + resiliencia declarativa (SmallRye Fault Tolerance).
 * Orden de aplicación (fijo por spec MicroProfile FT, no por orden de anotaciones en el código):
 * Fallback( Retry( CircuitBreaker( Timeout( llamada ) ) ) )
 */
@ApplicationScoped
public class CustomerGateway {

    private static final Logger LOG = Logger.getLogger(CustomerGateway.class);

    private final CustomerClient client;

    CustomerGateway(@RestClient CustomerClient client) {
        this.client = client;
    }

    @Timeout(value = 800, unit = ChronoUnit.MILLIS)
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 1.0, delay = 5000, delayUnit = ChronoUnit.MILLIS, successThreshold = 1)
    @Retry(maxRetries = 2, delay = 100, delayUnit = ChronoUnit.MILLIS)
    @Fallback(fallbackMethod = "unknownCustomer")
    public Uni<Customer> find(Long id, long delayMs) {
        return client.get(id, delayMs)
                .invoke(c -> LOG.debugf("Cliente %d obtenido", id));
    }

    // Mismo tipo de retorno y mismos parámetros que el método protegido.
    Uni<Customer> unknownCustomer(Long id, long delayMs) {
        LOG.warnf("Fallback para cliente %d", id);
        return Uni.createFrom().item(new Customer(id, "Desconocido", "N/A"));
    }
}
