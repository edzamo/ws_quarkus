package pe.yape.training.beneficiarios.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.SQLRestriction;


/**
 * Entidad JPA "pura". Campos públicos: Panache/Hibernate generan los accesores en build time.
 * No es un record ni se expone por la API (para eso están los DTO).
 */
@Entity
@SQLRestriction("active = true")
@Table(name = "beneficiary")
public class Beneficiary {

    @Id
    @GeneratedValue
    public Long id;

    @Column(name = "full_name", nullable = false, length = 120)
    public String fullName;

    @Column(nullable = false, unique = true, length = 9)
    public String phone;

    @Column(length = 40)
    public String alias;

    @Column(name = "document_number", unique = true, length = 8)
    public String documentNumber;

    @Column(nullable = false)
    public boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt = Instant.now();
}
