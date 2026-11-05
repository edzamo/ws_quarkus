package pe.yape.training.product;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.util.Map;

@ApplicationScoped
public class ProductRepository {

    private final Map<Long, Product> data = Map.of(
            1L, new Product(1L, "Cuenta Sueldo", new BigDecimal("0.00"), 1L),
            2L, new Product(2L, "Tarjeta Yape", new BigDecimal("15.90"), 2L));

    /** Uni vacío (null) = no existe. */
    public Uni<Product> findById(Long id) {
        return Uni.createFrom().item(data.get(id));
    }
}
