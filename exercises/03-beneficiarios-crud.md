# Ejercicio 3 — Beneficiarios Yape

Base: [projects/m03-beneficiarios](../projects/m03-beneficiarios). Levanta `./mvnw quarkus:dev` y usa Swagger UI.

## Parte A — Explorar
1. Abre `http://localhost:8080/q/swagger-ui` y ejecuta cada endpoint.
2. Crea un beneficiario con celular inválido y otro duplicado. Compara status y cuerpo.
3. Observa en el log el SQL generado por cada operación. ¿Cuántas sentencias hace un `PUT`?
4. Abre `/q/dev` → Hibernate ORM y revisa el esquema generado.

## Parte B — Extender (hazlo con TDD: primero el test)
5. Agrega el campo `documentNumber` (DNI, 8 dígitos, **opcional**, único si viene). Validación y 409 si se repite.
6. `GET /beneficiaries/by-phone/{phone}` → 200 o 404.
7. Agrega **soft delete**: columna `active` (boolean). `DELETE` marca inactivo; el listado solo muestra activos. *(Pista: `@SQLRestriction("active = true")` de Hibernate.)*
8. Devuelve en la lista un header `X-Total-Count` con el total (usa `repository.count()` o `query.count()`).
9. Ordenamiento configurable: `?sort=fullName` o `?sort=-createdAt`. Rechaza campos no permitidos con 400 (¡nunca pases el campo directo al HQL!).

## Parte C — Calidad
10. Prueba que dos `POST` simultáneos con el mismo celular no crean duplicados (usa `ExecutorService`; uno debe fallar con 409 o error controlado). ¿Qué protege realmente: el servicio o la BD?
11. Mueve el `ErrorResponse` para que incluya el campo `path` de la petición.
12. Contesta: ¿por qué `Beneficiary.createdAt` es `updatable = false`?

## Criterios de aceptación
- [ ] `./mvnw test` verde con tests nuevos para cada punto
- [ ] Ninguna respuesta de error es HTML ni stacktrace
- [ ] El contrato queda documentado en `/q/openapi`
- [ ] Ningún input del usuario se concatena en HQL
- [ ] Puedes explicar dirty checking y por qué los ids saltan de 50 en 50

## Bonus
Cambia a **Active Record** en una rama y compara cuántas líneas ahorras y qué pierdes en testabilidad.
