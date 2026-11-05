# Quarkus para Yape

Ruta de estudio de Quarkus para un equipo con experiencia en Spring Boot, NestJS, Kafka y Feature Toggles (BCP / Yape).
Base: curso de Udemy "Quarkus" (9 secciones · 70 clases), reordenado y ampliado con lo que pesa en producción.

**Stack:** Java 21 · Quarkus 3.40.1.1 · Maven (`./mvnw` en cada proyecto)

## Dónde está cada cosa

| Clase | Tema | Teoría | Ejercicio | Proyecto | Tests |
|---|---|---|---|---|---|
| 1 | Fundamentos: build time vs runtime, dev mode | [classes/01](classes/01-fundamentos.md) | [exercises/01](exercises/01-primer-proyecto.md) | [m01-hello](projects/m01-hello) | 3 |
| 2 | Configuración, perfiles y CDI | [classes/02](classes/02-configuracion-y-cdi.md) | [exercises/02](exercises/02-config-y-cdi.md) | [m02-config](projects/m02-config) | 7 |
| 3 | REST + Hibernate ORM con Panache | [classes/03](classes/03-rest-hibernate-panache.md) | [exercises/03](exercises/03-beneficiarios-crud.md) | [m03-beneficiarios](projects/m03-beneficiarios) | 13 |
| 4 | Reactividad: Mutiny, hilos, resiliencia, backpressure | [classes/04](classes/04-reactivo-mutiny.md) · [anexo](classes/04-anexo-reactividad-detalle.md) | [exercises/04](exercises/04-reactivo.md) | [m04-customer](projects/m04-customer) (:8082) + [m04-product](projects/m04-product) (:8080) | 4 + 6 |

> Las clases siguen el orden de estudio, no el del curso: la Clase 4 corresponde a la sección 6 del curso (reactivo).

**Cómo se trabaja:** concepto breve → equivalencia con Spring → proyecto ejecutable → ejercicio con criterios de aceptación. Clases en píldoras; el detalle va en anexos.

**Ejecutar un proyecto:**
```bash
cd projects/<proyecto>
./mvnw quarkus:dev     # modo desarrollo con live reload
./mvnw test            # tests
```
Para `m04-product` levanta antes `m04-customer`.

## Progreso

| Módulo | Estado |
|---|---|
| M1 Fundamentos | ✅ |
| M2 Configuración y CDI | ✅ |
| M3 REST + Panache (sin front Vue) | ✅ |
| **M6 Reactivo (Mutiny/Vert.x)** | 🟡 Clase 4 lista; **faltan los 3 retos del ejercicio 4** |
| M7 Persistencia reactiva | 🔲 **siguiente** |
| E1 Kafka · E2 Feature toggles · E3 Seguridad · E4 Observabilidad | 🔲 |
| M4 GraalVM · M5 Docker · M8 GraphQL · M9 Upgrades | 🔲 |
| E5 Testing · E6 Hexagonal · E7 Despliegue · E8 Integrador | 🔲 |

## Ruta pendiente

```
Ejercicio 4 → M7 → E1 → E2 → E3 → E4 → M4 → M5 → M8 → E5 → E6 → E7 → E8 → M9
```

Primero lo que más pesa en el día a día (reactivo, persistencia, Kafka, toggles, seguridad, observabilidad); después nativo, Docker y GraphQL.

### Módulos del curso por hacer

| Módulo | Qué veremos | Ejercicio |
|---|---|---|
| **M7 Persistencia reactiva** ⭐ | Hibernate Reactive con Panache, cliente SQL reactivo, Dev Services con PostgreSQL, **Flyway**, `@WithTransaction`; cuándo usar ORM clásico vs reactivo | Migrar `customer` a PostgreSQL con Hibernate Reactive + Flyway |
| **M4 GraalVM** | `./mvnw package -Dnative` (con `container-build`, sin instalar GraalVM), reflexión, `@RegisterForReflection`, Mandrel | Compilar un proyecto a nativo y medir JVM vs native |
| **M5 Docker** | Los 4 Dockerfiles que genera Quarkus, Jib, límites de recursos, probes, `quarkus-kubernetes` | Imagen JVM y nativa; comparar tamaños y verificar health |
| **M8 GraphQL** | SmallRye GraphQL (`@Query`, `@Mutation`), cliente tipado, N+1; cuándo **no** usarlo | Exponer `customer` y `product` por GraphQL |
| **M9 Upgrades** | `quarkus update` y breaking changes | Actualizar un proyecto antiguo |

### Módulos extra (no están en el curso, sí en nuestro stack)

| # | Módulo | Qué cubre | Ejercicio |
|---|---|---|---|
| E1 ⭐ | Kafka con SmallRye Reactive Messaging | `@Incoming`/`@Outgoing`, `Emitter`, DLQ, idempotencia, Dev Services | Eventos `PagoRealizado` con reintentos y DLQ |
| E2 ⭐ | Feature toggles | Unleash / OpenFeature, activación gradual, kill switch | Activar un flujo nuevo detrás de un toggle |
| E3 ⭐ | Seguridad | OIDC/JWT, `@RolesAllowed`, propagación de token | Proteger la API con JWT y roles |
| E4 ⭐ | Observabilidad | OpenTelemetry, Micrometer/Prometheus, logs JSON | Traza distribuida `customer` ↔ `product` |
| E5 | Testing a fondo | `@QuarkusIntegrationTest`, `@InjectMock`, tests de contrato | Cobertura con JaCoCo |
| E6 | Arquitectura hexagonal | Puertos/adaptadores con CDI, ArchUnit | Reestructurar `customer` |
| E7 | Despliegue | Kubernetes/OpenShift, ConfigMap/Secret | Desplegar en kind/minikube |
| E8 | Proyecto integrador | API + Kafka + Postgres + toggles + seguridad + observabilidad | Servicio tipo "Yapear" |

## Referencia rápida: Spring Boot → Quarkus

| Spring Boot | Quarkus |
|---|---|
| `@RestController`, `@GetMapping` | JAX-RS: `@Path`, `@GET` |
| `@Autowired`, `@Service` | CDI/ArC: `@Inject`, `@ApplicationScoped` (resuelto en **build time**) |
| `@ConfigurationProperties` / `@Value` | `@ConfigMapping` / `@ConfigProperty`; perfiles `%dev`, `%test`, `%prod` |
| Spring Data JPA | Hibernate ORM con Panache (Repository o Active Record) |
| WebFlux: `Mono` / `Flux` | Mutiny: `Uni` / `Multi` (`flatMap` → `chain`) |
| Actuator | SmallRye Health: `/q/health` |
| springdoc | SmallRye OpenAPI: `/q/swagger-ui` |
| Testcontainers | Dev Services |
| Spring Kafka | Reactive Messaging |
| `@RestControllerAdvice` | `@ServerExceptionMapper` |
| devtools | Dev Mode (`quarkus dev`) |

## Estructura del repositorio

```
ws_quarkus/
├── README.md        ← este índice
├── classes/         ← teoría por clase
├── exercises/       ← ejercicios y criterios de aceptación
└── projects/        ← código de cada clase (mXX-nombre)
```
