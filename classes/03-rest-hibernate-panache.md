# Clase 3 — API REST con Hibernate ORM + Panache

**Proyecto:** [projects/m03-beneficiarios](../projects/m03-beneficiarios) · **Ejercicio:** [exercises/03-beneficiarios-crud.md](../exercises/03-beneficiarios-crud.md)

Caso: CRUD de **Beneficiarios Yape** (a quién le puedo yapear), con búsqueda, paginación, validaciones y errores consistentes.

## 1. Extensiones nuevas

| Extensión | Para qué | Spring equivalente |
|---|---|---|
| `hibernate-orm-panache` | JPA/Hibernate con API simplificada | Spring Data JPA |
| `jdbc-h2` | BD en memoria para aprender | H2 |
| `hibernate-validator` | Bean Validation (`@NotBlank`, `@Pattern`) | `spring-boot-starter-validation` |
| `smallrye-openapi` | `/q/openapi` y Swagger UI | springdoc |

## 2. Arquitectura por capas del proyecto

```
api/        BeneficiaryResource   ← HTTP: rutas, status codes, DTOs
            BeneficiaryRequest/Response (records)
domain/     BeneficiaryService    ← negocio + @Transactional
            BeneficiaryRepository ← acceso a datos (Panache)
            Beneficiary           ← entidad JPA
error/      Excepciones + ErrorMappers ← traducción a HTTP
```

Regla: **la entidad nunca sale por la API**. Se expone un DTO (`record`), así cambiar la tabla no rompe el contrato.

## 3. Panache: dos estilos

| | Active Record | Repository (el usado aquí) |
|---|---|---|
| La entidad | `extends PanacheEntity` y trae `persist()`, `find()`… | POJO simple |
| Consulta | `Beneficiary.find("phone", x)` | `repository.find("phone", x)` |
| Cuándo | Prototipos, CRUD simple | Equipos con capas/hexagonal; más testeable y mockeable |
| Spring | — | `JpaRepository` |

Elegimos **Repository** porque encaja con la arquitectura hexagonal de Yape y facilita mocks.

### Consultas Panache
```java
find("phone", phone).firstResultOptional()                       // por campo
find("lower(fullName) like ?1", Sort.by("fullName"), "%ana%")    // HQL + orden
findAll(Sort.by("fullName")).page(Page.of(page, size)).list()    // paginación
deleteById(id)                                                   // boolean
```
HQL con parámetros posicionales (`?1`) o nombrados (`:name`). **Nunca concatenes input del usuario** en el HQL (inyección).

### Campos públicos
Con Panache los campos pueden ser `public`; en build time genera getters/setters y reescribe los accesos. Parece mala práctica, pero es una convención del framework.

## 4. Transacciones

```java
@Transactional                    // jakarta.transaction.Transactional (≈ el de Spring)
public Beneficiary update(Long id, BeneficiaryRequest req) {
    Beneficiary b = get(id);      // entidad "managed"
    apply(b, req);                // sin persist(): dirty checking guarda al commit
    return b;
}
```
- Pon `@Transactional` en el **servicio**, no en el recurso.
- Lecturas simples no la necesitan.
- Una `RuntimeException` hace **rollback**; una checked, no (igual que Spring).

## 5. Validaciones y errores

### Validación (en el DTO)
```java
public record BeneficiaryRequest(
    @NotBlank @Size(max = 120) String fullName,
    @NotBlank @Pattern(regexp = "9\\d{8}") String phone,
    @Size(max = 40) String alias) {}
```
Se activa con `@Valid` en el parámetro del endpoint. Si falla se lanza `ConstraintViolationException`.

### Mapeo de errores centralizado
```java
@ServerExceptionMapper
public RestResponse<ErrorResponse> notFound(BeneficiaryNotFoundException e) { ... 404 ... }
```
(≈ `@RestControllerAdvice`). Todas las respuestas de error tienen la misma forma: `{code, message, timestamp}`.

| Situación | Status |
|---|---|
| Creado | **201** + header `Location` |
| Validación falla | **400** `VALIDATION_ERROR` |
| No existe | **404** `NOT_FOUND` |
| Celular duplicado | **409** `DUPLICATE_PHONE` |
| Borrado | **204** |

La columna `phone` además tiene `unique=true`: la regla "duplicado" se verifica en el servicio **y** en la BD (la BD es la última línea de defensa ante carreras entre dos peticiones simultáneas).

## 6. Ciclo de vida del esquema (¡solo para aprender!)
```yaml
quarkus.hibernate-orm.database.generation: drop-and-create
```
Hibernate crea las tablas desde las entidades y **borra todo al reiniciar**. En producción se usa **Flyway/Liquibase** con migraciones versionadas (módulo 7).

Datos semilla solo en dev: `%dev.quarkus.hibernate-orm.sql-load-script: import-dev.sql`.

> Observación: en dev los ids son **1, 51, 101**… Hibernate usa una secuencia con *allocationSize = 50*: reserva bloques de ids para evitar ir a la BD en cada insert. No es un bug; no confíes en que los ids sean consecutivos.

## 7. OpenAPI / Swagger
- Contrato: `GET /q/openapi` · UI: `GET /q/swagger-ui` (por defecto solo en dev/test).
- Enriquecemos con `@Tag` y `@Operation`.
- Útil también para generar clientes y para contract testing.

## 8. Tests
- `@QuarkusTest` levanta la app real con H2 y usa RestAssured por HTTP.
- `@BeforeEach @Transactional` limpia la BD para que los tests sean independientes.
- 11 tests: crear, obtener, 404, validaciones (2), duplicado (2), actualizar, borrar, lista con búsqueda/paginación y OpenAPI.

## 9. Probar a mano (dev)
```bash
cd projects/m03-beneficiarios && ./mvnw quarkus:dev
curl localhost:8080/beneficiaries
curl localhost:8080/beneficiaries?q=ana
curl -i -X POST localhost:8080/beneficiaries -H 'Content-Type: application/json' \
     -d '{"fullName":"Rosa Díaz","phone":"999888777","alias":"Rosita"}'
```
Swagger UI: http://localhost:8080/q/swagger-ui · en los logs verás el SQL (`%dev` lo activa).

## 10. Preguntas de repaso
1. ¿Por qué usamos `record` DTO en vez de devolver la entidad?
2. ¿Por qué validar el celular duplicado en el servicio si la BD ya tiene `unique`?
3. ¿Por qué `update` no llama a `persist()`?
4. ¿Qué pasa con los datos al reiniciar con `drop-and-create` y por qué no sirve en prod?
5. Si `size=100000` en la lista, ¿qué protección pusimos?

<details><summary>Respuestas</summary>

1. Desacopla contrato público del modelo de datos, evita exponer campos internos y problemas de serialización perezosa.
2. Para dar un error de negocio claro (409) en el caso normal; la restricción en BD cubre las carreras concurrentes.
3. La entidad está *managed* dentro de la transacción: Hibernate detecta el cambio (dirty checking) y hace el UPDATE al commit.
4. Se pierden; en producción necesitas migraciones controladas y datos persistentes.
5. `Math.min(size, 100)` limita el tamaño de página.
</details>

## Siguiente
Clase 4: **Programación reactiva con Mutiny y Vert.x** (módulo 6 del plan). Antes de eso conviene cerrar el ejercicio 3.
