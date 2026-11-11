package pe.yape.training.customer;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Entidad Active Record reactiva: los métodos estáticos (find, listAll, count...)
 * devuelven Uni/Multi en vez de bloquear.
 *
 * Usamos PanacheEntityBase + IDENTITY (en vez de PanacheEntity) porque PanacheEntity genera el id
 * con una SECUENCIA `customer_seq` (allocationSize 50) que nuestra migración V1 no crea.
 * Con Flyway el esquema es del SQL: la entidad debe coincidir con él, no al revés.
 */
@Entity
public class Customer extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String name;
    public String tier;
    public String phone;
}
