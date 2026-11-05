# Ejercicio 2 — Configuración y CDI

Base: [projects/m02-config](../projects/m02-config). Corre `./mvnw quarkus:dev` y trabaja con live reload.

## Parte A — Configuración
1. Cambia `exchange.spread` en `%dev` a `0.05` y comprueba el efecto sin reiniciar.
2. Sobreescribe con variable de entorno: `EXCHANGE_SPREAD=0.10 ./mvnw quarkus:dev`. ¿Cuál valor ganó y por qué?
3. Agrega la moneda `BRL: 0.95` solo en `%dev` (pista: se puede definir en el bloque del perfil).
4. Agrega a `ExchangeConfig` un `Optional<String> promoCode()` y un `@WithDefault("10000") BigDecimal maxAmount()`.
   Rechaza con 400 los montos mayores a `maxAmount`.
5. Quita `exchange.base-currency` del yml y arranca. Lee el error y vuelve a ponerlo.

## Parte B — CDI
6. Crea un tercer proveedor `RandomRateProvider` (tasa ± 1%) activo con `exchange.provider=random`. Verifica con `-Dexchange.provider=random` en el build.
7. Agrega un interceptor `@Cached` simple (un `ConcurrentHashMap` por moneda con TTL de 30 s usando el `Clock` inyectado).
8. En un test, reemplaza el `Clock` por uno fijo con `@InjectMock` o un producer alternativo (`@Alternative @Priority(1)` en `src/test`) y verifica el `timestamp` exacto.

## Parte C — Preguntas
9. ¿Qué diferencia hay entre `@IfBuildProperty` y `@LookupIfProperty`? Impleméntala con `Instance<RateProvider>` y compara.
10. Explica con tus palabras por qué la config puede romper el build o el arranque y por qué eso es una ventaja.

## Criterios de aceptación
- [ ] `./mvnw test` en verde con los tests nuevos
- [ ] `maxAmount` y `promoCode` implementados con mapping tipado
- [ ] Al menos 3 proveedores seleccionables por propiedad
- [ ] Test con reloj fijo
- [ ] Puedes contar la precedencia de fuentes de config sin mirar la clase

## Bonus
Crea un perfil `qa` (`%qa`) y arráncalo con `-Dquarkus.profile=qa`.
