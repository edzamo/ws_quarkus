# Clase 1 — Fundamentos de Quarkus

**Proyecto:** [projects/m01-hello](../projects/m01-hello) · **Ejercicio:** [exercises/01-primer-proyecto.md](../exercises/01-primer-proyecto.md)

## 1. ¿Qué problema resuelve Quarkus?

Java tradicional (Spring Boot incluido) hace mucho trabajo **al arrancar**: escanea el classpath, lee anotaciones por reflexión, crea proxies y construye el grafo de beans. Eso cuesta segundos y memoria. Para un monolito que vive semanas está bien; para microservicios que escalan en Kubernetes, o funciones serverless que nacen y mueren, es caro.

**Idea central de Quarkus: mover ese trabajo de runtime a build time.**

```
Spring Boot                          Quarkus
-----------                          -------
build:   compilar + empaquetar       build:   compilar + ANALIZAR + generar bytecode
runtime: escanear, reflexión,        runtime: cargar lo ya resuelto y arrancar
         proxies, grafo de beans
```

Consecuencias prácticas:

| | Efecto |
|---|---|
| Arranque | ms en vez de segundos |
| Memoria | menos RSS por instancia → más pods por nodo → menos costo |
| Native | al no usar reflexión dinámica, compilar con GraalVM es viable |
| Trade-off | errores de DI/config aparecen en **build**, y las librerías necesitan una *extensión* Quarkus |

## 2. Modos de ejecución

| Modo | Comando | Cuándo |
|---|---|---|
| **Dev** | `./mvnw quarkus:dev` | Desarrollo: live reload, Dev UI, Dev Services |
| **JVM** | `java -jar target/quarkus-app/quarkus-run.jar` | Producción estándar |
| **Native** | `./mvnw package -Dnative` | Arranque ultrarrápido / serverless (módulo 4) |

## 3. Anatomía del proyecto

```
m01-hello/
├── pom.xml                        BOM de Quarkus + plugin quarkus-maven-plugin
├── mvnw                           Maven wrapper (usa este, no el Maven global)
└── src/
    ├── main/
    │   ├── java/.../hello/
    │   │   ├── GreetingService.java    bean CDI
    │   │   └── GreetingResource.java   endpoint JAX-RS
    │   ├── resources/application.yml   configuración
    │   └── docker/                     4 Dockerfiles ya generados (módulo 5)
    └── test/java/...                   @QuarkusTest
```

Extensiones elegidas al crear el proyecto (`quarkus create app`):

| Extensión | Para qué |
|---|---|
| `rest-jackson` | REST + JSON (JAX-RS sobre Vert.x). Antes se llamaba *RESTEasy Reactive* |
| `smallrye-health` | `/q/health`, `/q/health/live`, `/q/health/ready` (≈ Actuator) |
| `smallrye-openapi` | `/q/openapi` y `/q/swagger-ui` |
| `config-yaml` | Permite `application.yml` |

> Todo lo propio de Quarkus cuelga de `/q/`. Con `quarkus extension list` / `quarkus extension add <nombre>` agregamos más.

## 4. Spring → Quarkus en código

```java
// Spring                              // Quarkus
@RestController                        @Path("/hello")
@RequestMapping("/hello")
@GetMapping                            @GET
@RequestParam(defaultValue="mundo")    @QueryParam("name") @DefaultValue("mundo")
@Service                               @ApplicationScoped
@Autowired                             @Inject
@SpringBootTest                        @QuarkusTest
MockMvc                                RestAssured (given().when().get())
```

Detalles que sorprenden viniendo de Spring:
- `@Inject` funciona en campos **package-private**; no hace falta `private` + constructor ni `public`. Quarkus evita reflexión generando código en build time.
- Los endpoints devuelven el tipo directo; `@Produces` define el content-type.
- El puerto de test es **8081** (el de dev/prod es 8080).

## 5. Dev Mode: el mayor cambio de productividad

```bash
cd projects/m01-hello
./mvnw quarkus:dev
```

- Edita `GreetingService` y haz `curl localhost:8080/hello` → el cambio se aplica **sin reiniciar**.
- `r` fuerza un reinicio, `w` abre la app en el navegador, `d` abre la Dev UI, `h` muestra la ayuda.
- Dev UI: `http://localhost:8080/q/dev` (extensiones, config, beans CDI, health).
- `http://localhost:8080/q/swagger-ui` para probar la API.
- Con la extensión de tests continuos, pulsar `r` en la consola corre los tests al guardar.

## 6. Resultado medido (esta máquina, JVM mode)

| Métrica | Valor |
|---|---|
| Arranque | **0.58 s** |
| RSS | **~116 MB** |
| Extensiones | cdi, rest, rest-jackson, smallrye-health, smallrye-openapi, vertx… |

En el ejercicio comparamos esto con native (módulo 4) y con un Spring Boot equivalente.

## 7. Preguntas de repaso

1. ¿Por qué Quarkus necesita menos memoria que Spring Boot en JVM mode?
2. ¿Qué pasa si un `@Inject` no tiene bean candidato: en qué momento falla en Quarkus?
3. ¿Por qué un framework basado en reflexión dinámica es difícil de compilar a nativo?
4. ¿Qué diferencia hay entre `/q/health/live` y `/q/health/ready` y cuál usarías como liveness probe de Kubernetes?

<details><summary>Respuestas</summary>

1. Porque el escaneo, los proxies y el grafo de beans se resuelven en build; en runtime solo se carga el resultado.
2. En **build time** (el build falla con `UnsatisfiedResolutionException`), no al levantar en producción.
3. GraalVM hace análisis estático de "mundo cerrado": no ve clases cargadas por reflexión a menos que se declaren.
4. `live` indica que el proceso no está colgado (reiniciar si falla); `ready` que puede recibir tráfico (sacar del balanceador si falla). Liveness → `live`, readiness → `ready`.
</details>

## Siguiente

Clase 2: configuración (`application.yml`, perfiles, `@ConfigMapping`) y CDI en profundidad.
