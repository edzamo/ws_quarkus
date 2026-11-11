# m05-customer-db

`customer` de la clase 4 migrado a **PostgreSQL** con **Hibernate Reactive Panache + Flyway**. Puerto 8082.

```bash
./mvnw quarkus:dev     # Dev Services levanta Postgres solo (requiere Docker)
./mvnw test            # 6 tests contra un Postgres real
```

- Esquema y datos semilla: `src/main/resources/db/migration/V1__*.sql`, `V2__*.sql` (Hibernate **no** crea tablas).
- Teoría: [classes/05-persistencia-reactiva.md](../../classes/05-persistencia-reactiva.md) · Ejercicio: [exercises/05-persistencia-reactiva.md](../../exercises/05-persistencia-reactiva.md)
