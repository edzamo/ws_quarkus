# Ejercicio 6 — Kafka: eventos, idempotencia y DLQ

Base: [e1-payments](../projects/e1-payments). Docker corriendo.

## Parte A — Explorar
1. `./mvnw quarkus:dev`; envía el mismo pago 2 veces y un pago de 20000. Observa el log de cada caso.
2. Con `docker ps` localiza Kafka. ¿Qué topics existen tras el pago rechazado?
3. Cambia `failure-strategy` a `fail`, repite el pago de 20000 y luego envía uno válido. ¿Qué pasa? Vuelve a `dead-letter-queue`.

## Parte B — Extender (TDD)
4. Agrega `key = origen` al enviar (`Message.of(pago).addMetadata(OutgoingKafkaRecordMetadata...)`) y explica qué garantiza.
5. Un segundo consumidor `@Incoming("payments-dlq-in")` que lea `payments-dlq` y registre el motivo.
6. Reintentos: ante un fallo transitorio, reintenta 3 veces con `@Retry` antes de hacer `nack`. Test con el conector en memoria.
7. Persiste los ids procesados en Postgres (reutiliza lo de la clase 5) con clave única, en lugar del `Set` en memoria.

## Criterios
- Tests verdes; ningún pago duplicado procesado dos veces aunque reinicies la app (punto 7).
- Respuestas de repaso en el README del proyecto.
