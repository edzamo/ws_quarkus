# Clase 5 — Persistencia reactiva: Hibernate Reactive + PostgreSQL + Flyway

**Proyecto:** [m05-customer-db](../projects/m05-customer-db) (puerto 8082) · **Ejercicio:** [exercises/05-persistencia-reactiva.md](../exercises/05-persistencia-reactiva.md)

> Cierra el hueco de la clase 4: ahí `customer` usaba un `Map` en memoria. Ahora la cadena reactiva llega hasta la base de datos.

## 1. El problema: JDBC bloquea

Un endpoint `Uni` corre en el event loop. Si por dentro usas JDBC/JPA clásico, el hilo se queda esperando a la BD y congelas a las demás peticiones (píldora 5 de la clase 4). Hay que usar un **driver no bloqueante**.

| Spring | Quarkus |
|---|---|
| Spring Data JPA (bloqueante) | Hibernate ORM + Panache (clase 3) |
| Spring Data R2DBC | **Hibernate Reactive + Panache** / cliente `reactive-pg-client` |
| Flyway / Liquibase | Flyway / Liquibase (igual) |
| Testcontainers a mano | **Dev Services** (automático) |

## 2. Tres opciones de acceso a datos

| Opción | Cuándo |
|---|---|
| **Hibernate ORM clásico** (bloqueante) | CRUD con poca concurrencia; combínalo con virtual threads si quieres escalar |
| **Hibernate Reactive Panache** ⭐ | Cadena reactiva completa y modelo de entidades |
| **Cliente SQL reactivo puro** (`Pool`, `PgPool`) | Consultas muy específicas, máximo control, sin ORM |

`quarkus-spring-data-jpa`: sirve para migrar código Spring existente; no para código nuevo.

## 3. Extensiones

```
quarkus-hibernate-reactive-panache   # ORM reactivo
quarkus-reactive-pg-client           # driver reactivo de Postgres (lo usa Hibernate Reactive)
quarkus-flyway + quarkus-jdbc-postgresql   # migraciones
```

⚠ **Flyway usa JDBC** (corre una vez al arrancar, bloqueando está bien); la app usa el cliente reactivo. Por eso hay dos drivers.

## 4. Dev Services

Sin configurar URL/usuario, en `dev` y `test` Quarkus levanta **PostgreSQL en un contenedor** y conecta todo (Docker debe estar corriendo). En prod defines `%prod.quarkus.datasource.reactive.url` y `%prod.quarkus.datasource.jdbc.url`.

## 5. Panache reactivo

Igual que la clase 3, pero todo devuelve `Uni`:

```java
Customer.<Customer>findById(id)        // Uni<Customer>
Customer.<Customer>listAll(Sort.by("id"))  // Uni<List<Customer>>
Customer.deleteById(id)                // Uni<Boolean>
```

### Transacciones: la regla nueva
- **Escrituras** → `Panache.withTransaction(() -> ...)` (o `@WithTransaction`). Sin transacción, falla.
- **Lecturas simples** → no hace falta.
- Una transacción es una **cadena**: la entidad que modificas debe cargarse *dentro* del lambda para que el dirty checking funcione.

```java
Panache.withTransaction(() -> Customer.<Customer>findById(id)
        .onItem().ifNull().failWith(() -> notFound(id))
        .invoke(c -> c.name = dto.name()))   // UPDATE al commit, sin persist()
```

## 6. Flyway: el esquema es del SQL

```
src/main/resources/db/migration/V1__create_customer.sql
                                V2__seed_customers.sql
```
- Versionadas e **inmutables**: nunca edites una migración ya aplicada; crea `V3__...`.
- `quarkus.hibernate-orm.schema-management.strategy: none` → Hibernate no toca el DDL (se acabó `drop-and-create`).
- Imprescindible en banca: auditoría de cambios, mismo esquema en todos los ambientes.

### Lo que nos mordió en la demo
`PanacheEntity` genera el id con la secuencia `customer_seq` (allocation 50). Como nuestra V1 usa `BIGSERIAL`, falló con `relation "customer_seq" does not exist`. Solución: `PanacheEntityBase` + `@GeneratedValue(IDENTITY)`. **La entidad debe coincidir con el SQL.**

## 7. Tests
`@QuarkusTest` + Dev Services = tests contra **Postgres real** (no H2): detectan diferencias de dialecto, constraints y migraciones rotas. Cuestan más (~1 min la primera vez por el contenedor).

## 8. Trampas
- Llamar a `persist()` fuera de `withTransaction` → error en runtime.
- Mezclar JPA bloqueante y Reactive en la misma app: dos stacks distintos, evítalo.
- Cargar relaciones perezosas fuera de la sesión: usa `fetch join` o `Mutiny.fetch(...)`.
- Editar una migración aplicada → Flyway falla por checksum.

## 9. Preguntas de repaso
1. ¿Por qué Flyway usa JDBC si la app es reactiva?
2. ¿Qué pasa si modificas `V1__...sql` después de aplicada?
3. ¿Por qué cargar la entidad dentro de `withTransaction` en el `PUT`?
4. ¿Qué ventaja tiene probar con Postgres real frente a H2?
5. CRUD con baja concurrencia: ¿Hibernate Reactive o ORM clásico?

<details><summary>Respuestas</summary>

1. Solo corre al arrancar y una vez; el bloqueo no afecta al event loop de las peticiones.
2. Falla al arrancar por *checksum mismatch*; se crea una migración nueva.
3. Para que la entidad esté *managed* en esa sesión y Hibernate detecte el cambio al commit.
4. Mismo dialecto, tipos, constraints y SQL de migraciones que en producción.
5. ORM clásico (con virtual threads si hace falta): menos complejidad; reactivo cuando toda la cadena lo es y hay alta concurrencia.
</details>

## Siguiente
**Extra E1: Kafka con SmallRye Reactive Messaging** (ruta sugerida: M7 → E1). Antes, cierra el ejercicio 5.
