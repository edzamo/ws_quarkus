# Ejercicio 5 — Persistencia reactiva

Base: [m05-customer-db](../projects/m05-customer-db). Necesitas Docker corriendo.

## Parte A — Explorar
1. `./mvnw quarkus:dev` y mira en el log cómo Dev Services arranca Postgres y Flyway aplica V1 y V2.
2. Con `docker ps` localiza el contenedor; conéctate (`docker exec -it <id> psql -U quarkus`) y revisa la tabla `flyway_schema_history`.
3. Haz un `PUT` y observa el SQL (activa `quarkus.hibernate-orm.log.sql=true`). ¿Cuántas sentencias hace?
4. Quita `Panache.withTransaction` del `POST` y observa el error.

## Parte B — Extender (TDD: primero el test)
5. Migración `V3`: agrega `phone VARCHAR(9) UNIQUE` (nullable). Expónlo en el DTO con validación y devuelve **409** si se repite.
6. `GET /customers/by-tier/{tier}` → `Multi`/lista filtrada con `Customer.list("tier", tier)`.
7. `GET /customers?page=0&size=10` con `PanacheQuery.page(...)`; limita `size` a 100.
8. Rompe a propósito: edita `V1__...sql` ya aplicada y arranca de nuevo. Documenta el error y cómo se resuelve correctamente.

## Parte C — Integración
9. Haz que `m04-product` consuma este `customer` (puerto 8082) y verifica que `/products/{id}/summary` sigue funcionando con datos de Postgres.

## Criterios de aceptación
- Todos los tests verdes contra Postgres (Dev Services).
- Ninguna migración editada tras aplicarse (solo nuevas).
- Respuestas a las preguntas de repaso de la clase en el README del proyecto.

## Bonus
Reemplaza `PanacheEntity`-style por el cliente SQL puro (`io.vertx.mutiny.pgclient.PgPool`) en un endpoint `GET /customers/count-by-tier` con `GROUP BY` y compara.
