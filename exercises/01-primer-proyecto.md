# Ejercicio 1 — Primer proyecto y live reload

Proyecto base ya creado: [projects/m01-hello](../projects/m01-hello). Los puntos 1–3 son para que los hagas tú.

## Parte A — Live reload
1. Levanta `./mvnw quarkus:dev`.
2. Cambia el mensaje de `GreetingService.greet` y verifica con `curl "localhost:8080/hello?name=Ana"` **sin reiniciar**.
3. Rompe el código a propósito (error de compilación) y observa cómo reacciona Dev Mode. Corrígelo.
4. Abre la Dev UI (`/q/dev`) y localiza el bean `GreetingService`.

## Parte B — Extender la API
5. Añade `GET /hello/{name}` con `@PathParam`.
6. Crea `GET /hello/info` que devuelva un JSON `{"app":"m01-hello","version":"1.0.0"}` usando un `record`.
   *Pista: `@Produces(MediaType.APPLICATION_JSON)`; no hace falta código de serialización.*
7. Añade un test `@QuarkusTest` por cada endpoint nuevo.

## Parte C — Medición
8. `./mvnw package -DskipTests` y arranca con `java -jar target/quarkus-app/quarkus-run.jar`.
9. Anota: tiempo de arranque (línea `started in`) y RSS (`ps -o rss= -p <PID>`).
10. Opcional: crea un proyecto equivalente en Spring Initializr (Web + Actuator) y compara.

| Métrica | Quarkus JVM | Spring Boot | Quarkus native (módulo 4) |
|---|---|---|---|
| Arranque | 0.58 s (base) | | |
| RSS | ~116 MB (base) | | |

## Criterios de aceptación
- [ ] `./mvnw test` en verde
- [ ] `/q/health` devuelve `UP`
- [ ] Un endpoint con `@PathParam` y otro que devuelve JSON
- [ ] Tabla de métricas completada
- [ ] Puedes explicar con tus palabras por qué el arranque es más rápido

## Bonus
Cambia el puerto a 9090 **solo en dev** con `application.yml` (`"%dev":` → `quarkus.http.port`).
