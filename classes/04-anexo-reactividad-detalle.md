# Anexo — Reactividad en detalle (versión larga)

> Material de consulta de la Clase 4 ([resumen conciso aquí](04-reactivo-mutiny.md)). Se puede borrar sin afectar la clase.

## 1. Reactividad: conceptos y decisiones en píldoras

> Cada píldora se lee en 2 minutos y se apoya en la anterior.

---

### Píldora 1 — La cafetería (qué es, de verdad)

Un cliente pide un café y la máquina tarda 2 minutos.

- **Modelo bloqueante (hilo por petición):** un cajero por cliente. Cada uno pide el café y se queda mirando la máquina. Con 1.000 clientes, 1.000 cajeros (hilos). Cada hilo cuesta ~1 MB de memoria.
- **Modelo no bloqueante (event loop):** un solo cajero toma el pedido, deja el ticket en la máquina y **atiende al siguiente**. Cuando suena la máquina, entrega el café.

```
Bloqueante:    [hilo-1 ████ espera BD ████]  [hilo-2 ████ espera ████]  … 1 hilo por petición
No bloqueante: [event-loop ▌▌ ▌ ▌▌ ▌ ▌▌ ▌▌ ]  ← 1 hilo atiende todo; solo trabaja, no espera
```

**Si solo recuerdas esto:** lo que cambia es **qué hace el hilo mientras espera**. Sin esperas, no hay nada que optimizar.

#### ⚠️ Ojo con el vocabulario: son dos ejes distintos

"Imperativo" y "reactivo" se usan juntos por costumbre, pero describen cosas diferentes:

| Eje | Pregunta | Opciones |
|---|---|---|
| **Estilo de código** | ¿Doy pasos o describo qué quiero? | **Imperativo** (`if`, `for`, línea tras línea) vs **declarativo** (pipeline: `.map().chain()…`) |
| **Modelo de ejecución** | ¿El hilo se queda esperando o se libera? | **Bloqueante** vs **no bloqueante** |

|  | Bloqueante | No bloqueante |
|---|---|---|
| **Imperativo** | Spring MVC + JPA clásico | `async/await`, corrutinas; virtual threads (*) |
| **Declarativo** | `Stream` de Java en memoria | **Mutiny, WebFlux** |

**En resumen:** *imperativo* = das los pasos uno por uno (`if`, `for`). *Declarativo* = describes qué debe pasar con los datos y el framework lo ejecuta. La **programación reactiva** es declarativa **y** no bloqueante.

- La cafetería explica el **modelo de ejecución** (el segundo eje).
- En este documento, "imperativo" en las decisiones de arquitectura significa **un hilo por petición con código secuencial** (hilos de plataforma o virtuales). Cuando hablemos de cómo se escribe, usaremos "imperativo vs declarativo".
- (*) Los virtual threads (Java 21) muestran por qué los ejes son independientes: escribes código imperativo y el hilo virtual "espera", pero lo que se libera es el **hilo del sistema** que lo sostiene, así que escala como un modelo no bloqueante.

---

### Píldora 2 — Ya lo conoces: `Mono`/`Flux` → `Uni`/`Multi`

Si usaste WebFlux (por ejemplo, en un GraphQL reactivo), **ya sabes el 80%**. Mutiny es Reactor con otro vocabulario.

| Spring WebFlux (Reactor) | Quarkus (Mutiny) | Qué hace |
|---|---|---|
| `Mono<T>` | `Uni<T>` | 0 ó 1 valor, o un error |
| `Flux<T>` | `Multi<T>` | 0 a N valores |
| `Mono.just(x)` | `Uni.createFrom().item(x)` | crear con un valor |
| `.map(...)` | `.map(...)` | transformar (síncrono) |
| `.flatMap(...)` | `.chain(...)` | encadenar otra operación asíncrona |
| `.switchIfEmpty(...)` | `.onItem().ifNull().switchTo(...)` | si no hay valor |
| `.onErrorResume(...)` | `.onFailure().recoverWithUni(...)` | recuperarse de un error |
| `.doOnNext(...)` | `.invoke(...)` | efecto lateral (log) |
| `Mono.zip(a, b)` | `Uni.combine().all().unis(a, b)` | paralelo y juntar |
| `.timeout(...)` | `.ifNoItem().after(...).fail()` | límite de tiempo |

```java
// WebFlux                                        // Quarkus + Mutiny
return repo.findById(id)                          return repo.findById(id)
  .switchIfEmpty(Mono.error(new NotFound()))        .onItem().ifNull().failWith(NotFound::new)
  .flatMap(p -> customers.find(p.ownerId())         .chain(p -> customers.find(p.ownerId())
      .map(c -> new Detail(p, c)));                       .map(c -> new Detail(p, c)));
```

**Igual:** es perezoso (nada corre hasta que alguien se suscribe; en un endpoint, el framework lo hace por ti) y no se debe bloquear el event loop.

**Distinto:**
1. Mutiny agrupa los operadores por evento (`onItem()`, `onFailure()`); Reactor usa una lista plana.
2. Un `Uni` puede emitir `null` (`ifNull()`); un `Mono` vacío no admite `null`.
3. En WebFlux todo el endpoint debe ser reactivo; en Quarkus, si devuelves un tipo directo corre en un worker y puedes bloquear.
4. El mismo JAX-RS (`@GET`, `@Path`) sirve para reactivo e imperativo; solo cambia el tipo de retorno.
5. `mutiny-reactor` convierte `Mono`/`Flux` a `Uni`/`Multi` si necesitas convivir.

**Si solo recuerdas esto:** `Mono` = `Uni`, `Flux` = `Multi`, `flatMap` = `chain`.

---

### Píldora 3 — Cuatro mitos que te hacen decidir mal

| Mito | Realidad |
|---|---|
| "Reactivo = asíncrono de negocio" | **No.** "Procesar el pago después" se resuelve con eventos (Kafka, colas, `202 Accepted`). Reactivo es **cómo usa los hilos el proceso**, no un patrón de integración. |
| "Reactivo es más rápido" | **No por petición.** Gana en **cuántas atiendes a la vez** con pocos recursos. |
| "Con Kafka debo ir reactivo" | Quarkus lo expone con `Multi` (Reactive Messaging), así que viene casi gratis. Pero un consumidor que hace JDBC por mensaje no gana con envolverlo. |
| "Si uso WebFlux/Mutiny ya soy reactivo" | Solo si **dentro de ese proceso** no hay bloqueos (ver Píldora 4). |

---

### Píldora 4 — La regla de oro: *"¿qué hace el hilo de ESTE proceso mientras espera?"*

La regla se evalúa **por microservicio**, nunca "de extremo a extremo".

```
                         ┌─► Kafka                      ✅ publico y sigo
Cliente ──► ORQUESTADOR ─┼─► SOA / REST externos        ✅ me suscribo; cuando responden, sigo
            (reactivo)   ├─► Microservicio X (JPA)      ✅ el bloqueo ocurre EN X, no en mí
                         └─► JPA/JPQL en mi propio
                             código                     ❌ AQUÍ bloqueo MI hilo
```

**El restaurante:** el orquestador es el **mesero**: entrega el pedido y atiende otras mesas, nunca se queda mirando el horno. El servicio con JPA es la **cocina**: sus cocineros sí esperan frente al horno, y es normal. El mesero atiende muchas mesas sin importar cómo trabaje la cocina.

Tres frases:
1. **Hacia afuera** (Kafka, REST, SOA, otro microservicio): me suscribo y sigo. Lo que el otro use por dentro no me afecta, porque la frontera de red desacopla los hilos.
2. **Hacia adentro** (mi propio proceso): nada bloqueante. JPA/JDBC/SDK síncrono en el mismo servicio **sí rompe**.
3. **Si necesito algo bloqueante adentro, lo aíslo** (`@Blocking`, `@RunOnVirtualThread`) o uso un driver reactivo.

Por eso: **el servicio de persistencia no necesita BD reactiva** para que el orquestador sea reactivo.

**Si solo recuerdas esto:** la cadena se rompe solo cuando **el mismo proceso** hace una espera que no libera su hilo.

---

### Píldora 5 — Tres escenarios con código: funciona, se rompe, se arregla

**✅ Funciona** — esperar a otro servicio con cliente no bloqueante:
```java
Uni<Customer> owner = customerClient.get(id);   // el hilo se libera mientras llega la respuesta
```

**❌ Se rompe** — JPA dentro de un endpoint reactivo:
```java
@GET
public Uni<List<Beneficiary>> list() {
    return Uni.createFrom().item(() -> repository.listAll());   // JPA bloqueante en el event loop
}
```
El event loop queda esperando la BD y congela a las demás peticiones que atiende. Quarkus lo detecta (`BlockedThreadChecker`) y en muchos casos lanza un error del tipo *"blocking operation not allowed on I/O thread"*.

**🔧 Cuatro formas de arreglarlo**

| Arreglo | Cómo | Cuándo |
|---|---|---|
| Devolver el tipo directo | `public List<Beneficiary> list()` → corre en worker | Lo más simple |
| `@Blocking` | Sobre el método, aunque devuelva `Uni` | Necesitas el `Uni` pero hay una parte bloqueante |
| `@RunOnVirtualThread` | Virtual thread (Java 21) | Código secuencial con escala alta |
| Driver reactivo | Hibernate Reactive / cliente SQL reactivo | Quieres cadena 100% no bloqueante (**Clase 5**) |

**Dentro de un mismo proceso, ¿qué es bloqueante?**

| Eslabón | No bloqueante | Bloqueante |
|---|---|---|
| Base de datos | Hibernate Reactive / cliente SQL reactivo | **JDBC, JPA/Hibernate ORM clásico** |
| Llamadas a servicios | Rest Client con `Uni` | `RestTemplate`, HTTP síncrono |
| Mensajería | Reactive Messaging | — |
| SDKs de terceros | solo los asíncronos | la mayoría |

---

### Píldora 6 — Caso real reconstruido: el GraphQL reactivo

*Lo reconstruyo a partir de lo que me contaste; no conozco los detalles reales de ese proyecto. Léelo como análisis, no como veredicto sobre nadie.*

```
Cliente ──► GraphQL (reactivo) ──► Business ──► otros servicios
```

Se decidió rehacerlo con el argumento *"no todo hacia abajo es reactivo"*. Como razón técnica **no se sostiene**: lo de aguas abajo está al otro lado de la red. Lo que realmente decide es una pregunta:

> **¿Cómo llamaba GraphQL a Business: con un cliente no bloqueante o con uno síncrono dentro de sus resolvers?**

| Situación real | ¿Había problema? | Qué tocaba |
|---|---|---|
| Cliente **no bloqueante** | **No**: la objeción era infundada | Mantener el diseño; proteger a Business con timeout/bulkhead |
| Cliente **síncrono** en el hilo reactivo | **Sí**, pero un *defecto de implementación* | Corregir ese punto (Píldora 5), no tirar el diseño |
| Diseño correcto pero el **equipo no podía mantenerlo** | Razón válida, pero **organizacional** | Decirlo con claridad y comparar con el beneficio medido |
| Nadie midió si hacía falta | Faltan datos | Prueba de carga antes de decidir |

GraphQL era además un **buen candidato**: es un agregador que resuelve varios servicios en paralelo, justo donde la composición de `Uni` aporta más. Y en SmallRye GraphQL un resolver que devuelve `Uni` corre en el event loop y uno que devuelve un tipo directo, en worker: **se puede mezclar**.

**Si solo recuerdas esto:** *"no todo es reactivo"* no es criterio de rechazo. Los criterios son **dónde se bloquea un hilo, cuánto cuesta mantenerlo y qué dicen las mediciones**. Ante un defecto concreto, se corrige el defecto.

---

### Píldora 7 — Los números: cuántas manzanas hay esperando

**Ley de Little:** peticiones en vuelo = peticiones por segundo × tiempo de espera.
(Si llegan 10 clientes por segundo y cada uno espera 2 s, siempre hay **20 esperando a la vez**.)

| Servicio | req/s | espera | En vuelo | Hilos clásicos |
|---|---|---|---|---|
| CRUD rápido sobre Postgres | 200 | 20 ms | 4 | 4 → sobran |
| Orquesta 3 APIs externas | 500 | 800 ms | 400 | ~400 → empieza a doler |
| Gateway con APIs lentas | 2.000 | 2 s | 4.000 | ~4.000 → inviable |

El reactivo importa cuando **muchas peticiones esperan a la vez**, no cuando hay muchas rápidas.

**Y ojo con proteger lo de abajo:** el orquestador puede tener 4.000 en vuelo sin gastar hilos, pero el servicio de persistencia (por ejemplo 200 hilos y un pool de 30 conexiones) **no**: se encola, sube su latencia y el orquestador recibe timeouts. Defensa desde el orquestador: `@Timeout`, `@CircuitBreaker`, `@Bulkhead`, `@Fallback`.
La BD suele ser el cuello de botella real: un pool de 30 conexiones sigue siendo 30, con o sin driver reactivo.

---

### Píldora 8 — Cómo decide un principal engineer

**Tres opciones reales en Java 21:**

| Opción | Se escribe | Escala en I/O | Cuándo |
|---|---|---|---|
| Imperativo clásico | Secuencial | Limitada por hilos | Concurrencia moderada, CRUD |
| **Imperativo + virtual threads** | **Igual de simple** | Muy alta | **Punto de partida recomendado**; JDBC/JPA valen |
| Reactivo (`Uni`/`Multi`) | Pipelines | Muy alta con pocos hilos | Streaming, backpressure, composición paralela |

Los virtual threads hicieron que "escalar I/O" ya no obligue a ir reactivo. Hoy el reactivo se justifica por **lo que aporta aparte de escalar**: streaming, backpressure (Píldora 10) y composición explícita de flujos.

**Árbol de decisión:**
```
¿Pasa la mayor parte del tiempo ESPERANDO I/O con alta concurrencia?
 ├─ No  → Imperativo. Fin.
 └─ Sí  → ¿Necesito streaming, backpressure o componer flujos en paralelo?
           ├─ No  → Imperativo + virtual threads.
           └─ Sí  → ¿Este proceso puede ser no bloqueante de punta a punta?
                     ├─ Sí → Reactivo.
                     └─ No → Imperativo + virtual threads, o reactivo aislando lo bloqueante con @Blocking.
```

**Seis preguntas antes de elegir:** ¿dónde se va el tiempo: CPU o espera? · ¿cuántas peticiones esperan a la vez en el pico? · ¿hay streaming o solo request-response? · ¿qué tecnología de datos tengo y cuánto cuesta cambiarla? · ¿mi equipo puede depurar reactivo a las 3 a.m.? · ¿qué dice la prueba de carga?

**Regla de oro del arquitecto:** empieza imperativo, **mide**, y pasa a reactivo donde la prueba muestre que el cuello de botella son hilos esperando. Decisión **por servicio**, no por empresa.

---

### Píldora 9 — Casos de uso: dónde sí, dónde no

| Caso | Veredicto | Por qué |
|---|---|---|
| **Home de la app** (saldo + movimientos + promos de 3 servicios) | ✅ Reactivo | Paralelo; tiempo ≈ el más lento; timeout y fallback por llamada |
| **Orquestador de pagos** (Kafka + REST + persistencia por HTTP) | ✅ Reactivo | Todo libera el hilo |
| **GraphQL agregador** | ✅ Reactivo | Composición paralela |
| **CRUD de beneficiarios** (Postgres + JPA, baja concurrencia) | ❌ Imperativo | Reactivo solo añade complejidad (`@Blocking` por todos lados) |
| **Servicio de scoring** (CPU pesada) | ❌ Imperativo | La CPU no mejora con reactivo |
| **Consumidor Kafka con INSERT JPA por evento** | ⚠️ Mezcla localizada | Consumo reactivo + trabajo JPA aislado con `@Blocking` |

**Caso de uso para construir: el Home de Yape (agregador).** Tres llamadas independientes, cada una con su límite de tiempo y su degradación, ejecutadas en paralelo:

```java
@GET
public Uni<Home> home() {
    Uni<String> saldo = balance()
            .ifNoItem().after(Duration.ofMillis(500)).fail()
            .onFailure().recoverWithItem("N/D");                       // el saldo es crítico: lo marcamos
    Uni<List<String>> movs = movements()
            .ifNoItem().after(Duration.ofMillis(500)).recoverWithItem(List.of());
    Uni<List<String>> promos = promos()
            .ifNoItem().after(Duration.ofMillis(300)).recoverWithItem(List.of());   // las promos son opcionales

    return Uni.combine().all().unis(saldo, movs, promos).asTuple()   // las 3 en PARALELO
            .map(t -> new Home(t.getItem1(), t.getItem2(), t.getItem3(), "N/D".equals(t.getItem1())));
}
```
Tiempo total ≈ **el más lento (≤ 500 ms)**, no la suma. Si las promos fallan, el home sigue; si el saldo falla, se marca degradado. Este patrón es el **Reto 1** del ejercicio.

---

### Píldora 10 — Backpressure (contrapresión): cuando el consumidor no da abasto

**En una frase:** es el mecanismo por el cual **el que consume le dice al que produce "más despacio, no te alcanzo"**, para que nada se sature.

**La manzana:** imagina una cinta transportadora. Un recolector pone 10 manzanas por segundo y el empacador solo puede empacar 2 por segundo. Sin control, las manzanas se acumulan sobre la cinta hasta que se caen (o se agota la memoria). Backpressure es el botón con el que el empacador pide frenar la cinta.

```
Productor (rápido) ──► [ cola ] ──► Consumidor (lento)
   10 eventos/s          ↑             2 eventos/s
                  ¿qué hacemos con el exceso?
```

**Cuando el productor es más rápido, hay 4 salidas:**

| Estrategia | Qué hace | Cuándo sirve |
|---|---|---|
| **Frenar al productor** (backpressure real) | El consumidor pide solo lo que puede procesar (`request(n)`) | Cuando el productor *puede* esperar (leer un archivo, un cursor de BD, Kafka) |
| **Buffer acotado** | Guarda un poco en cola | Picos cortos y pasajeros |
| **Descartar** (`drop`) | Tira lo que sobra | Datos que pierden valor (posición GPS, métricas, ticks) |
| **Fallar** | Lanza error | Cuando perder o acumular datos es inaceptable y debes enterarte |

> Nunca una cola **sin límite**: se llena la memoria y el servicio cae por un `OutOfMemoryError`.

**Verificado en este proyecto** (productor: 1 evento cada 10 ms, 50 eventos; consumidor: 100 ms por evento):

| Estrategia en Mutiny | Resultado real |
|---|---|
| Ninguna | **Falla** con `BackPressureFailure` |
| `onOverflow().buffer(5)` | **Falla** igual: el buffer se llena en cuanto el consumidor se atrasa |
| `onOverflow().drop()` | Procesa **5 de 50** (`0, 11, 22, 33, 44`) y descarta el resto |

```java
fastProducer
    .onOverflow().drop()                       // o .buffer(100), o .dropPreviousItems()
    .onItem().transformToUniAndConcatenate(this::slowWork)   // procesa de a uno
```

**Dónde lo encuentras en tu día a día:**
- **Kafka (Reactive Messaging):** si tu consumidor tarda, el conector **deja de pedir mensajes** hasta que acabes; Kafka los retiene en el broker. Es backpressure sin escribir código. Para publicar desde código, `Emitter` tiene `@OnOverflow(BUFFER | DROP | FAIL)`.
- **Streams / SSE / WebSocket:** un cliente lento no debe tumbar al servidor.
- **WebFlux:** los equivalentes son `onBackpressureBuffer`, `onBackpressureDrop` y `onBackpressureLatest`.

**Lo que NO es backpressure:**
- En un **request/response HTTP normal** (cliente A llama a servicio B), el backpressure **no viaja por la red**. Si A manda más de lo que B soporta, B se encola y A recibe timeouts. Ahí se protege con `@Timeout`, `@Bulkhead` y `@RateLimit` (Píldora 7).
- Un `Uni` no necesita backpressure: emite **un** valor. Es un concepto de `Multi` (flujos).

**Si solo recuerdas esto:** backpressure = *"que el consumidor lento pueda frenar al productor rápido"*. Si no lo tienes, eliges entre acumular (riesgo de caída), descartar (pérdida de datos) o fallar.


