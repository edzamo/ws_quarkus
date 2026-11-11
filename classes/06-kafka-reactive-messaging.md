# Clase 6 (E1) — Kafka con SmallRye Reactive Messaging

**Proyecto:** [e1-payments](../projects/e1-payments) (puerto 8083) · **Ejercicio:** [exercises/06-kafka.md](../exercises/06-kafka.md)

## 1. Equivalencias con Spring Kafka

| Spring Kafka | Quarkus |
|---|---|
| `KafkaTemplate.send()` | `Emitter<T>` con `@Channel("nombre")` |
| `@KafkaListener(topics=...)` | `@Incoming("canal")` |
| `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` | `failure-strategy: dead-letter-queue` |
| `spring.kafka.*` | `mp.messaging.incoming|outgoing.<canal>.*` |
| Testcontainers Kafka | **Dev Services** (contenedor automático) |

Idea clave: tu código habla con **canales** (nombres lógicos); la configuración los une a **topics** de Kafka.

```
POST /payments → Emitter ("payments-out") → topic payments → @Incoming("payments-in") → PaymentConsumer
                                                                 └─ nack → topic payments-dlq
```

## 2. Extensión
`quarkus-messaging-kafka`. Sin configurar el broker, Dev Services levanta uno en dev/test. Los serializadores JSON se detectan solos para tus `record`.

## 3. Productor: `Emitter`
```java
@Channel("payments-out") Emitter<PagoRealizado> emitter;
emitter.send(pago);   // CompletionStage: se completa cuando Kafka confirma
```
El endpoint responde **202 Accepted**: el evento se publicó, el procesamiento es asíncrono.

## 4. Consumidor: `@Incoming` y confirmación manual
Con `Message<T>` decides: `msg.ack()` (ok) o `msg.nack(error)` (falló).
Kafka entrega **al menos una vez** → el mismo mensaje puede llegar repetido → el consumidor debe ser **idempotente** (clave = `id` del evento; en producción, tabla con unique o Redis).

## 5. Fallos y DLQ
`failure-strategy` define qué pasa con un `nack`:

| Estrategia | Efecto |
|---|---|
| `fail` (por defecto) | Detiene el consumo del canal |
| `ignore` | Registra y sigue (pierdes el mensaje) |
| **`dead-letter-queue`** | Manda el mensaje a otro topic y sigue ⭐ |

El DLQ evita que **un mensaje venenoso bloquee toda la partición**; luego alguien lo revisa o reprocesa.

## 6. Lo que nos mordió en la demo
Con el YAML con claves con puntos (`dead-letter-queue.topic: payments-dlq`) el DLQ se creó como `dead-letter-topic-payments-in` (el valor se ignoró **sin error**). Anidándolo (`dead-letter-queue:` → `topic:`) funcionó y se verificó en el log: *"sending the record to a dead letter topic payments-dlq"*. **Verifica siempre el comportamiento real, no solo que arranque.**

## 7. Tests
- Conector **en memoria** (`InMemoryConnector`): rápidos, sin Docker; verifican lógica de productor y consumidor.
- Limitación: no prueba serialización real ni el DLQ; eso se comprueba con Kafka real (dev).

## 8. Trampas
- Procesar sin idempotencia → cobros duplicados.
- `ack` antes de terminar el trabajo → pérdida si falla después.
- Mensajes enormes o sin `id`/versión de esquema: rompen consumidores al evolucionar. (Avro + Schema Registry en producción.)
- Orden: solo se garantiza **dentro de una partición** (usa la misma *key* para el mismo cliente).

## 9. Preguntas de repaso
1. ¿Por qué el POST responde 202 y no 201?
2. ¿Qué pasa con un mensaje que siempre falla si la estrategia es `fail`?
3. ¿Por qué el consumidor debe ser idempotente si Kafka es confiable?
4. ¿Qué ventaja da `Message<T>` frente a recibir el payload directo?
5. ¿Qué no prueba el conector en memoria?

<details><summary>Respuestas</summary>

1. No hay recurso creado aún; el evento fue aceptado y se procesará después.
2. Detiene el canal: nada más se consume hasta intervenir.
3. "Al menos una vez": reintentos y rebalanceos generan duplicados.
4. Control explícito de ack/nack y acceso a metadatos (key, partición, offset).
5. Serialización real, configuración del broker y el DLQ.
</details>

## Siguiente
**E2 — Feature Toggles** (ruta: E1 → E2).
