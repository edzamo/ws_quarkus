# Ejercicio 4 — Reactivo y resiliencia (3 retos)

Base: [m04-customer](../projects/m04-customer) + [m04-product](../projects/m04-product). Corre ambos (`customer` primero).

## Reto 1 — Paralelismo medible
`GET /products/{id}/summary` debe consultar **en paralelo** tres fuentes independientes y combinarlas:
- el producto (repositorio),
- el cliente dueño (`customer`),
- un "score de riesgo" simulado que tarda 400 ms (`Uni` con `delayIt`).

Usa `Uni.combine().all().unis(...)`.
**Criterios:** el tiempo total es ≈ 400 ms (no 400 + lo demás). Test que lo **demuestre** con un umbral y un test que compruebe que con ejecución secuencial sería mayor. Explica por escrito por qué en este caso no usaste `chain`.

## Reto 2 — Circuit breaker y degradación elegante
Añade `@CircuitBreaker` a `CustomerGateway.find` (p. ej. abrir tras 4 fallos en ventana de 4 llamadas, 5 s abierto).
**Criterios:**
- Test que provoque la apertura del circuito y verifique que, con el circuito **abierto**, el fallback responde **sin llamar** al `customerClient` (`verify(..., never())` o conteo de invocaciones).
- Tras la espera, el circuito pasa a half-open y se recupera cuando el servicio vuelve a responder.
- Documenta con una tabla `timeout × reintentos` qué latencia máxima ve el usuario y por qué decidiste esos valores.

## Reto 3 — Cazar el bloqueo del event loop
Crea `GET /products/{id}/report` que (a propósito) haga `Thread.sleep(2000)` dentro de un `Uni` en el event loop.
1. Lanza 20 peticiones concurrentes a `/products/1` mientras llamas al report y mide cómo se degrada la latencia. Observa el aviso `BlockedThreadChecker` en el log.
2. Corrígelo de **tres formas** y compara cada una: (a) `@Blocking`, (b) `@RunOnVirtualThread`, (c) `Uni.createFrom().item(...).runSubscriptionOn(Infrastructure.getDefaultWorkerPool())`.
**Criterios:** tabla con latencias p50/p99 de `/products/1` bajo carga para el caso roto y para cada solución; conclusión escrita de cuándo elegirías cada una.

## Preguntas de cierre (responde en el README del proyecto)
- ¿Por qué un `Uni` sin suscriptor no ejecuta nada?
- ¿Qué ventaja y qué riesgo tienen los reintentos sobre un servicio ya degradado?

## Bonus
Expón `GET /customers/stream` en `customer` como `Multi` con Server-Sent Events (`@RestStreamElementType`) emitiendo un cliente por segundo, y consúmelo desde `product`.
