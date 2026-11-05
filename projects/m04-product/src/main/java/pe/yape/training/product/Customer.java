package pe.yape.training.product;

/** Copia local del contrato de customer (cada microservicio es dueño de su modelo). */
public record Customer(Long id, String name, String tier) {
}
