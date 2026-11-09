# m04-product

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/m04-product-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Reto 2 — Circuit breaker y degradación elegante

`CustomerGateway.find` tiene `@Timeout(800ms)` + `@CircuitBreaker(requestVolumeThreshold=4, failureRatio=1.0, delay=5000ms, successThreshold=1)` + `@Retry(maxRetries=2, delay=100ms)` + `@Fallback`.

Orden real de aplicación (lo define la spec de MicroProfile Fault Tolerance, no el orden de las anotaciones en el código):

```
Fallback( Retry( CircuitBreaker( Timeout( llamada ) ) ) )
```

### Por qué esos valores

- **`requestVolumeThreshold=4, failureRatio=1.0`**: abre cuando el 100% de una ventana de 4 llamadas falla — exactamente lo que pide el enunciado ("abrir tras 4 fallos en ventana de 4 llamadas"). Como el `@CircuitBreaker` queda **dentro** del `@Retry`, cada reintento de una misma petición HTTP también cuenta como una llamada para esta ventana (ver prueba en `CustomerGatewayCircuitBreakerTest`).
- **`delay=5000` (5 s)**: tiempo que el circuito permanece abierto antes de pasar a half-open, tal como pide el enunciado.
- **`successThreshold=1`**: con una sola respuesta exitosa en half-open el circuito cierra. Se eligió el valor más simple (default de la spec); un valor mayor sería más conservador pero añade latencia extra a la recuperación.
- **`@Timeout(800)` y `@Retry(maxRetries=2, delay=100)`**: ya existían en el proyecto (no se tocaron) — ver tabla de latencias abajo.

### Tabla: timeout × reintentos — latencia máxima que ve el usuario

| Escenario | Llamadas reales a `customerClient` | Latencia máxima vista por el usuario | Por qué |
|---|---|---|---|
| Circuito cerrado, cliente responde normal | 1 | ~tiempo real del servicio (ms) | Sin reintentos, sin timeout. |
| Circuito cerrado, fallo transitorio que se recupera | hasta 3 (1 + 2 reintentos) | hasta **~2.6 s** (`3 × 800ms` de timeout + `2 × 100ms` entre reintentos) | Peor caso: cada intento agota su `@Timeout` antes de reintentar. |
| Circuito cerrado, fallos sostenidos (aún no llega a 4 en la ventana) | hasta 3 por petición | igual que arriba, **~2.6 s** | El circuito todavía no decide abrir; el usuario paga el costo completo de reintentos + timeout. |
| **Circuito ABIERTO** (ya se alcanzaron los 4 fallos) | **0** | unos pocos **ms** (fallback inmediato, `CircuitBreakerOpenException` no espera timeout) | Esta es la ganancia del Reto 2: en vez de ~2.6 s por petición, el fallback responde casi instantáneo mientras el circuito está abierto. |
| Half-open (tras 5 s), llamada de prueba | 1 | igual que la fila 1 o 2 (depende de si el servicio ya se recuperó) | Solo se arriesga **una** llamada real; si falla, el circuito vuelve a abrir otros 5 s. |

**Conclusión:** sin circuit breaker, un servicio caído le cuesta a cada usuario ~2.6 s por petición indefinidamente. Con el circuito abierto, ese costo cae a unos pocos ms — el `@CircuitBreaker` convierte una degradación lenta y repetida en una degradación rápida y predecible (el patrón "fail fast").

Test: `CustomerGatewayCircuitBreakerTest.circuitoAbreSinLlamarAlClienteYLuegoSeRecuperaEnHalfOpen` — abre el circuito a propósito, verifica con `Mockito.verify(..., times(n))` que el cliente real deja de invocarse mientras está abierto, espera los 5 s reales y confirma la recuperación en half-open.

## Reto 3 — Cazar el bloqueo del event loop

`GET /products/{id}/report` hace `Thread.sleep(2000)` dentro de un `Uni` normal — a propósito mal hecho: ese código corre en el hilo del event loop de Vert.x (nadie le dijo que use otro hilo).

Para que el experimento sea determinista sin depender de cuántos núcleos tiene la máquina, el perfil `%test` fija `quarkus.vertx.event-loops-pool-size=1` (`application.yml`): con un solo hilo de event loop, bloquearlo bloquea literalmente todas las demás peticiones.

Se implementaron 3 variantes de arreglo además de la rota, todas expuestas como endpoints separados para poder compararlas en caliente:

| Variante | Endpoint | Mecanismo |
|---|---|---|
| Roto | `/products/{id}/report` | `Uni.createFrom().item(...)` normal — corre en el event loop. |
| (a) `@Blocking` | `/products/{id}/report/blocking` | Fuerza el método completo a un worker thread (`executor-thread-N`). |
| (b) `@RunOnVirtualThread` | `/products/{id}/report/virtual-thread` | Ejecuta en un hilo virtual (Java 21) por invocación. |
| (c) `runSubscriptionOn(...)` | `/products/{id}/report/worker-pool` | `Uni.createFrom().item(...).runSubscriptionOn(Infrastructure.getDefaultWorkerPool())` — mueve solo la suscripción, sin anotar el método. |

### Tabla de latencias p50/p99 de `/products/1` bajo carga (20 peticiones concurrentes mientras corre `/report`)

Medido en `ReportLoadTest` (ver salida real de `mvn test -Dtest=ReportLoadTest`, los números concretos varían ±10-20 ms entre corridas por ruido de máquina):

| Escenario | p50 | p99 |
|---|---|---|
| Roto (sin arreglo) | ~1900-2000 ms | ~1900-2000 ms |
| (a) `@Blocking` | ~20-30 ms | ~30-45 ms |
| (b) `@RunOnVirtualThread` | ~20-80 ms* | ~30-90 ms* |
| (c) `runSubscriptionOn(workerPool)` | ~30-70 ms | ~40-80 ms |

\* La primera vez que la JVM usa el mecanismo de hilos virtuales tiene un costo de inicialización de unos cientos de ms; el test hace un *warm-up* previo para no medir ese costo one-time, pero en producción (primera petición de la vida del proceso) ese costo sí aplicaría una vez.

### Conclusión: cuándo elegir cada una

- **`@Blocking`**: la opción por defecto para código bloqueante clásico (JDBC, librerías legacy que no se pueden cambiar) con concurrencia moderada. Es la más simple de razonar (toda la petición corre en un worker thread tradicional), pero ese pool de workers tiene tamaño acotado — bajo concurrencia muy alta (miles de peticiones bloqueantes simultáneas) se puede agotar y empezar a encolar.
- **`@RunOnVirtualThread`**: la mejor opción cuando se necesita **mucha** concurrencia de código bloqueante (miles de hilos "lógicos" a la vez), porque los hilos virtuales son baratísimos de crear comparados con hilos de plataforma. Ojo: solo escala bien si el código bloqueante realmente "desmonta" el hilo virtual (p. ej. `Thread.sleep`, I/O de red/disco estándar) — un `synchronized` alrededor de la llamada bloqueante "fija" (*pins*) el hilo virtual a su hilo portador y anula la ventaja.
- **`runSubscriptionOn(Infrastructure.getDefaultWorkerPool())`**: útil cuando **solo una parte** de un pipeline reactivo es bloqueante, no el método completo — da control fino a nivel de `Uni` en vez de marcar todo el endpoint. También es la opción natural fuera de un método de recurso JAX-RS (p. ej. dentro de un servicio de dominio), donde `@Blocking`/`@RunOnVirtualThread` no aplican directamente.

En los tres casos la latencia bajo carga cae de ~2 s a unas pocas decenas de ms — la lección central del Reto 3 es que **el problema nunca fue el `Thread.sleep`**, sino ejecutarlo en el hilo equivocado (el event loop compartido por todas las peticiones).

## Related Guides

- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it
- SmallRye Fault Tolerance ([guide](https://quarkus.io/guides/smallrye-fault-tolerance)): Build fault-tolerant network services
- SmallRye Health ([guide](https://quarkus.io/guides/smallrye-health)): Monitor service health
- YAML Configuration ([guide](https://quarkus.io/guides/config-yaml)): Use YAML to configure your Quarkus application
