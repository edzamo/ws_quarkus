package pe.yape.training.customer;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.time.Duration;

/**
 * Endpoints REACTIVOS: devuelven Uni (0..1 valor) o Multi (0..N valores).
 * Como retornan Uni/Multi, Quarkus los ejecuta en el EVENT LOOP (hilo no bloqueante).
 */
@Path("/customers")
@Produces(MediaType.APPLICATION_JSON)
public class CustomerResource {

    private final CustomerRepository repository;

    CustomerResource(CustomerRepository repository) {
        this.repository = repository;
    }

    /** delayMs simula un servicio lento (para probar timeouts y reintentos desde product). */
    @GET
    @Path("/{id}")
    public Uni<Customer> get(@PathParam("id") Long id, @QueryParam("delayMs") long delayMs) {
        Uni<Customer> customer = Uni.createFrom().item(() -> repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cliente no encontrado: " + id)));
        // Mutiny exige una duración > 0; sin retardo devolvemos el Uni tal cual.
        return delayMs > 0 ? customer.onItem().delayIt().by(Duration.ofMillis(delayMs)) : customer;
    }

    @GET
    public Multi<Customer> list() {
        return Multi.createFrom().iterable(repository.findAll());
    }
}
