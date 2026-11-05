package pe.yape.training.product;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
public class ProductResource {

    private final ProductRepository repository;
    private final CustomerGateway customers;

    ProductResource(ProductRepository repository, CustomerGateway customers) {
        this.repository = repository;
        this.customers = customers;
    }

    @GET
    @Path("/{id}")
    public Uni<Product> get(@PathParam("id") Long id) {
        return repository.findById(id)
                .onItem().ifNull().failWith(() -> new NotFoundException("Producto no encontrado: " + id));
    }

    /**
     * COMBINAR flujos: pide producto y cliente EN PARALELO y los junta.
     * El tiempo total ≈ el más lento, no la suma.
     */
    @GET
    @Path("/{id}/detail")
    public Uni<ProductDetail> detail(@PathParam("id") Long id, @QueryParam("customerDelayMs") long customerDelayMs) {
        Uni<Product> product = get(id);
        // El cliente depende del producto (ownerCustomerId) => encadenamos con chain/flatMap.
        return product.chain(p -> {
            Uni<Customer> owner = customers.find(p.ownerCustomerId(), customerDelayMs);
            return owner.map(c -> new ProductDetail(p, c, "N/A".equals(c.tier())));
        });
    }

    /** Muestra el hilo: Uni => event loop (vert.x-eventloop-thread-N). */
    @GET
    @Path("/threads/reactive")
    public Uni<Map<String, String>> reactiveThread() {
        return Uni.createFrom().item(() -> Map.of("thread", Thread.currentThread().getName()));
    }

    /** Retorno directo => worker thread (executor-thread-N); aquí SÍ se puede bloquear. */
    @GET
    @Path("/threads/blocking")
    public Map<String, String> blockingThread() {
        return Map.of("thread", Thread.currentThread().getName());
    }

    /** Un Uni forzado a worker thread con @Blocking (para código JDBC clásico, Thread.sleep, etc). */
    @GET
    @Path("/threads/forced-blocking")
    @Blocking
    public Uni<Map<String, String>> forcedBlocking() {
        return Uni.createFrom().item(() -> Map.of("thread", Thread.currentThread().getName()));
    }
}
