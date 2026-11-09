package pe.yape.training.product;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.time.Duration;
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

    @GET
    @Path("/{id}/summary")
    public Uni<ProductSummary> summary(
        @PathParam("id") Long id,
        @QueryParam("customerId") long customerId,
        @QueryParam("customerDelayMs") long customerDelayMs) {
      // Versión inline (todo en un solo pipeline, sin variables intermedias):
      // return Uni.combine().all()
      //     .unis(get(id), customers.find(customerId, customerDelayMs), riskScore(id))
      //     .with((p, c, r) -> new ProductSummary(p, c, r));

      Uni<Product> product = get(id);
      Uni<Customer> customer = customers.find(customerId, customerDelayMs);
      Uni<RiskScore> risk = riskScore(id);

      return Uni.combine().all()
          .unis(product, customer, risk)
          .with((p, c, r) -> new ProductSummary(p, c, r));
    }


  private Uni<RiskScore> riskScore(Long id) {
    return Uni.createFrom().item(() -> new RiskScore(id, "N/A", Math.random()))
        .onItem().delayIt().by(Duration.ofMillis(400));
  }

  private Uni<ProductSummary> combineAll(Uni<Product> product, Uni<Customer> customer, Uni<RiskScore> risk) {
    return Uni.combine().all()
        .unis(product, customer, risk).
        with((p, c, r) -> new ProductSummary(p, c, r));
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

    /**
     * Reto 3 — A PROPÓSITO está roto: Thread.sleep() dentro de un Uni normal corre en el
     * event loop (nadie le dijo que use otro hilo) y lo bloquea 2 s completos.
     */
    @GET
    @Path("/{id}/report")
    public Uni<Map<String, String>> report(@PathParam("id") Long id) {
        return Uni.createFrom().item(() -> buildReport(id));
    }

    /** Solución (a): @Blocking fuerza el método entero a un worker thread. */
    @GET
    @Path("/{id}/report/blocking")
    @Blocking
    public Uni<Map<String, String>> reportBlocking(@PathParam("id") Long id) {
        return Uni.createFrom().item(() -> buildReport(id));
    }

    /** Solución (b): hilo virtual (Java 21) por invocación, se puede bloquear sin costo de hilos de plataforma. */
    @GET
    @Path("/{id}/report/virtual-thread")
    @RunOnVirtualThread
    public Uni<Map<String, String>> reportVirtualThread(@PathParam("id") Long id) {
        return Uni.createFrom().item(() -> buildReport(id));
    }

    /** Solución (c): mover la suscripción (y por tanto el Thread.sleep) al worker pool por defecto de Mutiny. */
    @GET
    @Path("/{id}/report/worker-pool")
    public Uni<Map<String, String>> reportWorkerPool(@PathParam("id") Long id) {
        return Uni.createFrom().item(() -> buildReport(id))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    private static Map<String, String> buildReport(Long id) {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Map.of("report", "listo-" + id, "thread", Thread.currentThread().getName());
    }
}
