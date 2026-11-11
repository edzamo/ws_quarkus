# e1-payments

Kafka con SmallRye Reactive Messaging. Puerto 8083. `POST /payments` publica un `PagoRealizado` en el topic `payments`; `PaymentConsumer` lo procesa (idempotente) y manda los rechazados a `payments-dlq`.

```bash
./mvnw quarkus:dev   # Dev Services levanta Kafka (requiere Docker)
curl -X POST localhost:8083/payments -H 'Content-Type: application/json' \
     -d '{"id":"p-1","origen":"999111222","destino":"999333444","monto":50}'
curl localhost:8083/payments/processed
./mvnw test          # 3 tests con conector en memoria (sin Docker)
```

Teoría: [classes/06-kafka-reactive-messaging.md](../../classes/06-kafka-reactive-messaging.md) · Ejercicio: [exercises/06-kafka.md](../../exercises/06-kafka.md)
