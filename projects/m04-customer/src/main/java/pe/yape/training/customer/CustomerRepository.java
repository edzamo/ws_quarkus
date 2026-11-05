package pe.yape.training.customer;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** "BD" en memoria. En M7 pasa a Postgres con Hibernate Reactive. */
@ApplicationScoped
public class CustomerRepository {

    private final Map<Long, Customer> data = Map.of(
            1L, new Customer(1L, "Ana Torres", "GOLD"),
            2L, new Customer(2L, "Luis Quispe", "BASIC"),
            3L, new Customer(3L, "Rosa Díaz", "BASIC"));

    public Optional<Customer> findById(Long id) {
        return Optional.ofNullable(data.get(id));
    }

    public List<Customer> findAll() {
        return data.values().stream().sorted((a, b) -> a.id().compareTo(b.id())).toList();
    }
}
