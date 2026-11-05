# Clase 4 — Programación reactiva con Mutiny y Vert.x

**Proyectos:** [m04-customer](../projects/m04-customer) (puerto 8082) · [m04-product](../projects/m04-product) (puerto 8080) · **Ejercicios:** [exercises/04-reactivo.md](../exercises/04-reactivo.md)

## 1. Reactividad en píldoras

> Cada píldora: una idea, una analogía y cómo explicarla **sin lenguaje técnico**. Detalle y casos en el [anexo](04-anexo-reactividad-detalle.md).

---

### Píldora 1 — Síncrono vs asíncrono

**Cafetería:** pides un café que tarda 2 minutos.
- **Síncrono:** te quedas parado en el mostrador hasta que esté. Mientras, ese empleado no atiende a nadie más.
- **Asíncrono:** te dan un número, te sientes, y **te llaman cuando esté listo**. El empleado atiende a otros mientras tanto.

**Técnico:** el "empleado" es un **hilo**. Esperar sin hacer nada desperdicia hilos, y los hilos son caros (memoria).
**Para un no técnico:** *"asíncrono es no quedarse parado esperando."*

---

### Píldora 2 — Dos preguntas que se suelen confundir

| Pregunta | Opciones | Analogía |
|---|---|---|
| **¿Cómo se escribe?** | **Imperativo**: paso a paso (`if`, `for`). **Declarativo / por eventos**: "cuando pase X, haz Y" | Receta de cocina vs. dejar instrucciones al ayudante ("cuando suene el horno, sácalo") |
| **¿Cómo se ejecuta?** | **Bloqueante**: el hilo espera. **No bloqueante**: el hilo se libera | Quedarse junto al horno vs. atender otra mesa |

**Programación reactiva = declarativa + no bloqueante.**

|  | Bloqueante | No bloqueante |
|---|---|---|
| **Imperativo** | Spring MVC + JPA | virtual threads\* |
| **Declarativo** | `Stream` de Java | **Mutiny, WebFlux** |

\*El hilo virtual "espera", pero libera el hilo del sistema; por eso escala sin escribir código reactivo.

**Para un no técnico:** *"imperativo es seguir una receta; por eventos es dejar instrucciones para cuando algo ocurra."*

---

### Píldora 3 — Si usaste WebFlux, ya lo sabes

| WebFlux (Reactor) | Quarkus (Mutiny) |
|---|---|
| `Mono<T>` — 0 ó 1 valor | `Uni<T>` |
| `Flux<T>` — 0 a N valores | `Multi<T>` |
| `.flatMap()` | `.chain()` |
| `.switchIfEmpty()` | `.onItem().ifNull().switchTo()` |
| `.onErrorResume()` | `.onFailure().recoverWithUni()` |
| `Mono.zip(a, b)` | `Uni.combine().all().unis(a, b)` |

Igual: es perezoso (nada corre hasta que alguien se suscribe) y no se debe bloquear el event loop.
Distinto: en Quarkus el mismo `@GET` sirve para reactivo e imperativo; cambia solo el tipo de retorno.

---

### Píldora 4 — La regla: "¿qué hace el hilo de ESTE proceso mientras espera?"

```
                         ┌─► Kafka                    ✅ publico y sigo
Cliente ──► ORQUESTADOR ─┼─► APIs REST / SOA          ✅ me suscribo y sigo
            (reactivo)   ├─► Microservicio con JPA    ✅ el bloqueo ocurre EN ÉL, no en mí
                         └─► JPA en mi propio código  ❌ aquí bloqueo MI hilo
```

**Restaurante:** el mesero (orquestador) deja el pedido en cocina y atiende otras mesas. La cocina (servicio con JPA) espera frente al horno, y es normal.

- **Hacia afuera** (Kafka, REST, otro microservicio): me suscribo y sigo. Lo que el otro use por dentro no me afecta.
- **Hacia adentro** (mi propio proceso): nada bloqueante.
- Se evalúa **por servicio**, nunca "de extremo a extremo". El servicio de persistencia **no necesita BD reactiva**.

---

### Píldora 5 — Cuando se rompe y cómo arreglarlo

```java
@GET
public Uni<List<Beneficiary>> list() {
    return Uni.createFrom().item(() -> repository.listAll());   // ❌ JPA bloqueante en el event loop
}
```
Congela a las demás peticiones del mismo hilo. Arreglos:
1. Devolver el tipo directo (`List<…>`) → corre en un worker.
2. `@Blocking` sobre el método.
3. `@RunOnVirtualThread` (Java 21).
4. Driver reactivo (Hibernate Reactive, Clase 5).

---

### Píldora 6 — Backpressure (contrapresión)

**Manzanas:** un recolector pone 10 por segundo en una cinta; el empacador solo puede con 2. Sin control, la cinta se desborda. **Backpressure es el botón con el que el empacador pide frenar la cinta.**

Si el productor es más rápido, hay 4 salidas: **frenarlo**, **buffer acotado**, **descartar** o **fallar**. Nunca una cola sin límite (se acaba la memoria).

Probado aquí (1 evento/10 ms, consumidor 100 ms/evento): sin estrategia → falla (`BackPressureFailure`); `drop()` → procesa 5 de 50.

- Kafka lo hace solo: si tu consumidor tarda, el conector deja de pedir mensajes y el broker los retiene.
- En un request/response HTTP **no viaja por la red**: se protege con `@Timeout`, `@Bulkhead`, `@RateLimit`.

**Para un no técnico:** *"que el que recibe pueda decirle al que envía: más despacio."*

---

### Píldora 7 — ¿Cuándo usar reactivo?

**Lo que mejora:** cuántas peticiones atiendes **a la vez** con pocos recursos. *No* la velocidad de una sola petición.
Peticiones esperando = peticiones por segundo × tiempo de espera (10/s × 2 s = 20 esperando a la vez).

```
¿Muchas peticiones esperando I/O a la vez?
 ├─ No  → Imperativo. Fin.
 └─ Sí  → ¿Necesito streaming, backpressure o combinar flujos en paralelo?
           ├─ No  → Imperativo + virtual threads.
           └─ Sí  → ¿Este proceso puede ser no bloqueante de punta a punta?
                     ├─ Sí → Reactivo.
                     └─ No → Imperativo + virtual threads (o reactivo aislando lo bloqueante con @Blocking).
```

| Caso | Veredicto |
|---|---|
| Home de la app (saldo + movimientos + promos de 3 servicios) | ✅ Reactivo: se piden en paralelo |
| Orquestador (Kafka + REST + persistencia por HTTP) | ✅ Reactivo |
| GraphQL agregador | ✅ Reactivo |
| CRUD con JPA y poca concurrencia | ❌ Imperativo |
| Servicio de scoring (CPU pesada) | ❌ Imperativo |
| Consumidor Kafka con INSERT JPA por evento | ⚠️ Reactivo en el borde, `@Blocking` en lo pesado |

**Tres creencias a evitar:** *"reactivo = asíncrono de negocio"* (eso se resuelve con eventos: Kafka, `202 Accepted`) · *"reactivo es más rápido"* · *"no todo hacia abajo es reactivo, así que no conviene"* (lo de otro servicio está al otro lado de la red y no bloquea tus hilos).

**Regla:** empieza imperativo, **mide**, y pasa a reactivo donde la prueba de carga muestre hilos esperando. Se decide **por servicio**.

## 2. Mutiny: Uni y Multi

| Tipo | Emite | Equivale a (Reactor) | Ejemplo |
|---|---|---|---|
| `Uni<T>` | 0 ó 1 valor (o error) | `Mono<T>` | un cliente, una respuesta HTTP |
| `Multi<T>` | 0..N valores | `Flux<T>` | lista, stream de eventos |

**Es perezoso:** nada ocurre hasta que alguien se **suscribe**. En un endpoint, Quarkus se suscribe por ti.

### Operadores que usarás el 90% del tiempo
```java
uni.map(c -> c.name())                          // transforma el valor (síncrono)
uni.chain(c -> otroUni(c.id()))                 // encadena otra operación asíncrona (≈ flatMap)
uni.onItem().ifNull().failWith(() -> new NotFoundException())
uni.onFailure().recoverWithItem(defaultValue)   // recuperar de un error
uni.invoke(x -> log(x))                         // efecto lateral sin cambiar el valor
Uni.combine().all().unis(a, b).asTuple()        // ejecutar EN PARALELO y juntar
```
Regla: `map` si la función devuelve un valor; `chain` si devuelve otro `Uni`.

> La equivalencia con WebFlux (`Mono`/`Flux`, `flatMap`/`chain`…) está en la **Píldora 3**.

## 3. Modelo de hilos (lo más importante)

Quarkus decide el hilo según **lo que retorna** el endpoint:

| Retorna | Se ejecuta en | Puedes bloquear |
|---|---|---|
| `Uni` / `Multi` | **event loop** (`vert.x-eventloop-thread-N`) | ❌ NUNCA |
| Tipo directo (`Product`, `Map`…) | **worker** (`executor-thread-N`) | ✅ |
| `@Blocking` | worker, aunque devuelva `Uni` | ✅ |
| `@RunOnVirtualThread` | virtual thread (Java 21) | ✅ |

> ⚠️ **Bloquear el event loop** (`Thread.sleep`, JDBC clásico, llamadas HTTP síncronas) congela todas las peticiones que atiende ese hilo. Quarkus lo detecta y avisa con `BlockedThreadChecker`.

Verificado en el proyecto: `/products/threads/reactive` → `vert.x-eventloop-thread-4`; `/products/threads/blocking` → `executor-thread-1`.

(La elección entre reactivo y virtual threads está en la Píldora 7.)

## 4. Comunicar microservicios

### Cliente REST declarativo
```java
@RegisterRestClient(configKey = "customer")
@Path("/customers")
public interface CustomerClient {
    @GET @Path("/{id}") Uni<Customer> get(@PathParam("id") Long id);
}
```
```yaml
quarkus.rest-client.customer.url: http://localhost:8082
```
Se inyecta con `@RestClient`. Equivale a OpenFeign pero reactivo y generado en build time.

### Combinar flujos
```java
product.chain(p -> customers.find(p.ownerCustomerId(), delay)
                            .map(c -> new ProductDetail(p, c, degraded)));
```
Aquí el cliente **depende** del producto, así que se encadena. Si fueran independientes, `Uni.combine().all()` los lanza en paralelo y el tiempo total ≈ el más lento.

## 5. Resiliencia (SmallRye Fault Tolerance)

```java
@Timeout(800)                                        // no esperar más de 800 ms
@Retry(maxRetries = 2, delay = 100)                  // reintentar 2 veces
@Fallback(fallbackMethod = "unknownCustomer")        // plan B si todo falla
public Uni<Customer> find(Long id, long delayMs) { ... }
```
Orden de aplicación: **Fallback( Retry( Timeout( llamada ) ) )**. El método fallback debe tener la misma firma.
Otros: `@CircuitBreaker`, `@Bulkhead`, `@RateLimit`. Funcionan con `Uni` sin bloquear.

### Resultado medido (con ambos servicios corriendo)

| Escenario | Tiempo | Respuesta |
|---|---|---|
| Customer normal | 0.52 s (primer request, en frío) | `degraded: false` |
| Customer lento 300 ms | 0.32 s | `degraded: false` |
| Customer 3 s (timeout 800 ms × 3 intentos + esperas 2×100 ms) | **2.73 s** | `owner: Desconocido`, `degraded: true` |

El tercer caso se explica sumando: 3 × 800 + 2 × 100 ≈ 2.6 s. Los reintentos **multiplican** la latencia: cuida que `timeout × (retries+1)` no supere el timeout del llamador.

## 6. Testear con mocks
```java
@InjectMock @RestClient CustomerClient customerClient;
when(customerClient.get(anyLong(), anyLong())).thenReturn(Uni.createFrom().failure(new RuntimeException("boom")));
```
Requiere `quarkus-junit5-mockito`. Así pruebas timeout, retry y fallback **sin levantar el otro servicio**. 10 tests en total (4 customer + 6 product).

## 7. Trampas
- `delayIt().by(Duration.ZERO)` lanza `IllegalArgumentException` (la duración debe ser > 0). Me pasó construyendo la clase.
- No mezcles `.await().indefinitely()` dentro de un endpoint reactivo: bloquea el event loop.
- Un `Uni` que nadie suscribe **no hace nada** (ni siquiera la llamada HTTP).
- Un `Uni` con `null` es "vacío" (`ifNull()`), no un error.

## 8. Repaso rápido
1. ¿Qué hilo ejecuta un endpoint que retorna `Uni<T>` y por qué importa?
2. ¿Cuándo `map` y cuándo `chain`?
3. Con `@Timeout(800)` + `@Retry(2)`, ¿cuánto tarda en el peor caso con un servicio colgado?
4. ¿Por qué mockear el `CustomerClient` y no usar el servicio real en tests?
5. Un servicio hace CRUD sobre Postgres con JPA y tiene baja concurrencia: ¿reactivo? ¿Por qué?
6. ¿Por qué un JDBC bloqueante dentro de un flujo reactivo anula la ventaja?
7. Con virtual threads en Java 21, ¿cuándo sigue justificándose el reactivo?

<details><summary>Respuestas</summary>

1. El event loop; si bloqueas ahí, congelas otras peticiones.
2. `map` si devuelves un valor; `chain` si devuelves otro `Uni`.
3. ≈ 3 × 800 ms + esperas entre reintentos ≈ 2.6 s.
4. Tests rápidos, deterministas y capaces de simular fallos y lentitud a voluntad.
5. No: hay pocas peticiones esperando a la vez y JPA es bloqueante; la cadena no sería reactiva y el costo en complejidad no compensa.
6. Porque el hilo del event loop queda esperando, congelando a las demás peticiones que atiende; la cadena es tan fuerte como su eslabón más bloqueante.
7. Cuando necesitas streaming, backpressure o composición explícita de flujos y tienes la cadena completa no bloqueante.
</details>

## Siguiente
Clase 5: **persistencia reactiva** (Hibernate Reactive con Panache + PostgreSQL con Dev Services + Flyway).
