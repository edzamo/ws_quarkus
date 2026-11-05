# Quarkus para Yape — Plan de estudio

> Ruta de aprendizaje de Quarkus para un equipo con experiencia en Spring Boot, NestJS, Kafka y Feature Toggles (BCP / Yape).
> Temario base: curso de Udemy "Quarkus" (9 secciones · 70 clases · 11 h 42 m), reorganizado y ampliado con foco en producción.

## Cómo trabajamos

- **Formato de cada clase:** concepto → equivalencia con Spring Boot → demo guiada → ejercicio → criterios de aceptación.
- **Un proyecto por módulo**, dentro de `projects/mXX-nombre/`. Cada uno se puede ejecutar de forma aislada.
- **Teoría en** `classes/`, **ejercicios en** `exercises/`.
- Regla de oro: no avanzamos de módulo sin haber resuelto el ejercicio y entendido el porqué de cada decisión.

## Mapa mental: Spring Boot → Quarkus

| Spring Boot | Quarkus | Nota clave |
|---|---|---|
| `@RestController`, `@GetMapping` | JAX-RS (`@Path`, `@GET`) con `quarkus-rest` | Estándar Jakarta REST |
| `@Autowired` / Spring DI | CDI / ArC (`@Inject`, `@ApplicationScoped`) | Resolución en **build time**, no en runtime |
| `application.yml` + profiles | `application.properties` + `%dev`, `%test`, `%prod` | MicroProfile Config (`@ConfigProperty`) |
| Spring Data JPA | Hibernate ORM con **Panache** / Spring Data compat | Active Record o Repository |
| Spring WebFlux / Reactor | **Mutiny** (`Uni`, `Multi`) + Vert.x | Reactivo sobre Netty/Vert.x |
| Actuator | SmallRye Health + Micrometer / Prometheus | `/q/health`, `/q/metrics` |
| springdoc / Swagger | SmallRye OpenAPI | `/q/swagger-ui`, `/q/openapi` |
| Testcontainers manual | **Dev Services** | Levanta DB/Kafka automáticamente en dev y test |
| Spring Kafka | SmallRye Reactive Messaging (`@Incoming`/`@Outgoing`) | Canales declarativos |
| Spring Cloud Config / FF | Config sources + Unleash / OpenFeature | Para feature toggles |
| `spring-boot-devtools` | **Dev Mode** (`quarkus dev`) con live reload | Muy superior en feedback |
| GraalVM (opcional, lento) | Native first-class | Arranque en ms, menos memoria |

## Extensiones del ecosistema (imagen del curso)

Quarkus se compone de **extensiones** (https://github.com/quarkusio/quarkus/tree/main/extensions). Las que veremos o conviene conocer:

Eclipse Vert.x · Eclipse MicroProfile · Spring (compat) · Hibernate · RESTEasy · Apache Camel · Kubernetes · OpenShift · Jaeger (tracing) · Prometheus · Apache Kafka · Netty.

---

## Temario

Leyenda: ✅ = hecho · 🔲 = pendiente · ⭐ = alta prioridad para Yape

### Módulo 1 — Fundamentos y conceptos (curso sec. 1 · 8 clases · 1 h 07)

Temas del curso:
- Quarkus: características principales y propuesta de valor
- Cómo abordar el curso, errores comunes y consejos
- Arquitectura tradicional vs. arquitecturas dockerizadas Java con compilación nativa
- Arquitecturas dockerizadas con orquestador Kubernetes orientadas a cloud / serverless
- **Optimización de desempeño: boot time y memoria** ⭐
- Arquitecturas serverless, pago por consumo real (Serverless y IaaS)
- Prerrequisitos, instalación y configuración del IDE y plugins
- Quiz de conceptualización

Qué vamos a profundizar:
- Build-time vs. runtime: por qué Quarkus arranca más rápido que Spring.
- JVM mode vs. native mode: cuándo conviene cada uno.
- Instalación: JDK 21, Maven/Gradle, Quarkus CLI, IntelliJ + plugin Quarkus.

**Ejercicio 1:** crear el primer proyecto con `quarkus create app`, levantar `quarkus dev`, modificar un endpoint con live reload y comparar tiempo de arranque y RSS de memoria contra un Spring Boot equivalente.

### Módulo 2 — El framework, extensiones e IntelliJ (curso sec. 2 · 10 clases · 1 h 31)

> Nota: esta sección aparece colapsada en las capturas, sin detalle de clases. Cubriremos lo esperable por título.

- Anatomía de un proyecto Quarkus (BOM, plugin Maven, estructura).
- Sistema de extensiones: `quarkus extension add/list`, code.quarkus.io.
- Configuración: `application.properties`, perfiles, `@ConfigProperty`, `@ConfigMapping`.
- Inyección de dependencias con CDI (ArC): scopes, qualifiers, producers, interceptores.
- Dev Mode, Dev UI (`/q/dev`) y Dev Services.
- Trabajo con IntelliJ: run configs, debugging, hot reload.

**Ejercicio 2:** servicio de "tipo de cambio" con configuración por perfil (`%dev`/`%prod`), un bean CDI inyectado y un `@ConfigMapping` tipado.

### Módulo 3 — Quarkus Web: app Full Stack con Vue.js e Hibernate (sec. 3 · 11 clases · 1 h 22)

Temas del curso:
1. Arquitectura del caso de estudio (aplicación de contactos)
2. Entidad base con Hibernate ORM y H2
3. API REST: método GET para listar contactos
4. Recursos GET, POST, PUT y DELETE (con asistente de IA)
5. Swagger / OpenAPI y pruebas con curl ⭐
6. Frameworks y patrones web embebibles en Quarkus para el front
7. Web Bundler: crear y servir una vista HTML
8. Formulario de contacto con Vue.js
9. Vista CRUD (listar, modificar, eliminar)
10. Recursos estáticos (imágenes, estilos)
11. Conclusiones y recomendaciones

Qué vamos a profundizar (más allá del curso):
- **Hibernate ORM with Panache** (Active Record vs. Repository).
- Validación con Hibernate Validator (`@Valid`), manejo de errores con `ExceptionMapper`.
- DTOs/records vs. entidades expuestas.
- Pruebas con `@QuarkusTest` y RestAssured.

**Ejercicio 3 (proyecto):** API de **Contactos / Beneficiarios Yape** — CRUD completo con Panache + H2, validaciones, OpenAPI y tests. El front Vue es opcional/bonus: el foco es el backend.

### Módulo 4 — Compilación nativa con GraalVM (sec. 4 · 8 clases · 1 h 02)

Temas del curso:
- GraalVM: características y ciclo de vida
- Prerrequisitos e instalación (Windows y Linux; en macOS usaremos SDKMAN)
- Prueba de compilación nativa con una clase Java simple
- Compilación nativa de un proyecto Quarkus y solución de errores
- Compilación nativa con GraalVM en Linux, pruebas y análisis
- (Entrevista en vivo con agente de IA — contenido no técnico)

Qué vamos a profundizar:
- `./mvnw package -Dnative` y `-Dquarkus.native.container-build=true` (sin instalar GraalVM local).
- Reflexión, `@RegisterForReflection`, recursos y errores típicos de native.
- Mandrel vs. GraalVM CE.
- Medición: tiempo de arranque, memoria, tamaño de binario, tiempo de build.

**Ejercicio 4:** compilar el proyecto del módulo 3 a nativo, medir métricas y documentar una tabla JVM vs. native.

### Módulo 5 — Dockerizar aplicaciones Quarkus (sec. 5 · 6 clases · 1 h 04)

Temas del curso:
- Importancia de Docker para desarrollo y DevOps
- Instalación de Docker Desktop / Linux
- Tipos de imágenes Docker y repositorios base (native, uber-jar, legacy)
- Imagen Docker con uber-jar y cómo evitar errores
- Empaquetado de compilaciones nativas en Docker (native y native-micro)

Qué vamos a profundizar:
- Los 4 Dockerfiles que genera Quarkus (`src/main/docker/`): `jvm`, `legacy-jar`, `native`, `native-micro`.
- `quarkus-container-image-jib` / `-docker` para construir sin Dockerfile.
- Límites de memoria/CPU en contenedor, probes de liveness/readiness (`/q/health`).
- Despliegue en Kubernetes con la extensión `quarkus-kubernetes` (manifiestos generados).

**Ejercicio 5:** generar imagen JVM y nativa, comparar tamaños, correr ambas con `docker compose` y verificar health checks.

### Módulo 6 — Programación reactiva con Vert.x y Mutiny (sec. 6 · 10 clases · 1 h 44) ⭐

Temas del curso:
- Caso de estudio y arquitectura de la solución
- Microservicio de productos
- **Reto práctico:** crear el microservicio Customer
- ¿Qué es la programación reactiva? Síncrono vs. asíncrono
- Eventos y aplicación al caso de estudio
- Mutiny y Vert.x: principales métodos para manejar flujos
- Refactor del microservicio Product
- Analizar, validar y probar Customer
- Comunicar microservicios (3 partes): Customer reactivo, Product con cliente reactivo, combinar flujos de datos

Qué vamos a profundizar:
- **Modelo de hilos:** event loop vs. worker threads; `@Blocking` / `@NonBlocking`; qué NO hacer en el event loop.
- `Uni` vs. `Multi`; `onItem().transform()`, `flatMap`/`chain`, `Uni.combine().all()`, `onFailure().recoverWithItem()`, `ifNoItem().after().fail()`.
- Rest Client reactivo (`quarkus-rest-client`), timeouts, retry (`@Retry`), circuit breaker y fallback (SmallRye Fault Tolerance) ⭐.
- Virtual threads (`@RunOnVirtualThread`) como alternativa a reactivo: cuándo elegir cada uno.

**Ejercicio 6 (proyecto):** dos microservicios, `customer` y `product`, que se comunican de forma reactiva; combinar ambos flujos en un endpoint agregador con timeout y fallback.

### Módulo 7 — Persistencia y ORM reactivos (sec. 7 · 7 clases · 1 h 45)

Temas del curso:
- Motor de base de datos persistente con Docker
- Soporte para Spring Data (compatibilidad)
- Blaze Persistence: introducción e implementación (2 partes)
- Clientes SQL reactivos nativos con Postgres
- **Hibernate Reactive con Panache** (opción más recomendada) ⭐

Qué vamos a profundizar:
- Dev Services con PostgreSQL (sin docker-compose manual).
- Migraciones con **Flyway** o Liquibase (imprescindible en banca).
- Transacciones reactivas (`@WithTransaction`, `Panache.withTransaction`).
- Cuándo usar Hibernate ORM clásico vs. Reactive vs. cliente SQL puro (`quarkus-reactive-pg-client`).
- Compatibilidad `quarkus-spring-data-jpa`: útil para migrar, no para código nuevo.

**Ejercicio 7:** migrar `customer` a PostgreSQL con Hibernate Reactive Panache + Flyway, con tests sobre Dev Services.

### Módulo 8 — GraphQL (sec. 8 · 8 clases · 1 h 46)

Temas del curso:
- Qué es GraphQL y conceptos principales
- API GraphQL y refactor del microservicio Customer
- Métodos de búsqueda y queries
- Mutaciones
- Práctica 02: API GraphQL
- Refactor de Product, modo debug en otro puerto
- Cliente GraphQL: Customer ↔ Product
- Búsqueda de un producto con parámetro

Qué vamos a profundizar:
- SmallRye GraphQL: `@GraphQLApi`, `@Query`, `@Mutation`, `@Source`.
- Cliente tipado (`quarkus-smallrye-graphql-client`).
- N+1 y batching; cuándo GraphQL **no** es la mejor opción frente a REST.

**Ejercicio 8:** exponer `customer` y `product` por GraphQL y consumir uno desde el otro con cliente tipado.

### Módulo 9 — Actualizaciones y recursos adicionales (sec. 9 · 3 clases · 17 min)

- Cómo actualizar a la última versión del framework (`quarkus update`).
- Proceso de upgrade en el servicio Product.
- Cierre del curso.

**Ejercicio 9:** correr `quarkus update` sobre un proyecto antiguo, revisar el diff y resolver breaking changes.

---

## Módulos extra (no están en el curso, pero son críticos en Yape)

El curso no cubre varias piezas de nuestro stack real. Los añadimos al final de la ruta:

| # | Módulo | Qué cubre | Ejercicio |
|---|---|---|---|
| E1 ⭐ | **Kafka con SmallRye Reactive Messaging** | `@Incoming`/`@Outgoing`, `Emitter`, serialización Avro/JSON, DLQ, idempotencia, Dev Services Kafka | Producir y consumir eventos `PagoRealizado` con reintentos y DLQ |
| E2 ⭐ | **Feature Toggles** | `quarkus-unleash` / OpenFeature, toggles por usuario/porcentaje, kill switch | Activar un flujo nuevo de forma gradual detrás de un toggle |
| E3 ⭐ | **Seguridad** | OIDC/JWT, `@RolesAllowed`, propagación de token entre servicios | Proteger la API con JWT y roles |
| E4 ⭐ | **Observabilidad** | OpenTelemetry (trazas), Micrometer/Prometheus, logging JSON, health | Traza distribuida entre `customer` y `product` |
| E5 | **Testing a fondo** | `@QuarkusTest`, `@QuarkusIntegrationTest`, `@InjectMock`, Dev Services, test de native | Cobertura con JaCoCo y tests de contrato |
| E6 | **Arquitectura hexagonal en Quarkus** | Puertos/adaptadores con CDI, módulos, ArchUnit | Reestructurar `customer` en hexagonal |
| E7 | **Despliegue** | Kubernetes/OpenShift, config por ConfigMap/Secret, perfiles | Desplegar en un cluster local (kind/minikube) |
| E8 | **Proyecto integrador** | Servicio tipo "Yapear": API + Kafka + Postgres + toggles + seguridad + observabilidad | Entregable final |

## Ruta sugerida

```
M1 → M2 → M3 → M6 → M7 → E1 → E2 → E3 → E4 → M4 → M5 → M8 → E5 → E6 → E7 → E8 → M9
```

Reordenada respecto al curso: dejamos **nativo, Docker y GraphQL** para después de dominar lo que más pesa en el día a día (REST, reactivo, persistencia, Kafka, toggles).

## Estructura del repositorio

```
ws_quarkus/
├── README.md            ← este documento (temario + progreso)
├── classes/             ← teoría de cada clase (01-fundamentos.md, ...)
├── exercises/           ← enunciados y criterios de aceptación
└── projects/            ← código de cada ejercicio (m01-hello, m03-contacts, ...)
```

## Progreso

| Módulo | Estado |
|---|---|
| M1 Fundamentos | ✅ Clase 1 + ejercicio (Parte A) |
| M2 Framework y extensiones | ✅ Clase 2 + ejercicio |
| M3 Quarkus Web + Hibernate | ✅ Clase 3 + ejercicio (sin front Vue) |
| M4 GraalVM | 🔲 |
| M5 Docker | 🔲 |
| M6 Reactivo (Vert.x/Mutiny) | 🟡 Clase 4 + proyectos listos, falta ejercicio |
| M7 Persistencia reactiva | 🔲 |
| M8 GraphQL | 🔲 |
| M9 Upgrades | 🔲 |
| E1–E8 Extras Yape | 🔲 |
