# Clase 2 — Configuración, perfiles y CDI

**Proyecto:** [projects/m02-config](../projects/m02-config) · **Ejercicio:** [exercises/02-config-y-cdi.md](../exercises/02-config-y-cdi.md)

Caso: convertidor de tipo de cambio PEN → USD/EUR con margen (spread) configurable por ambiente.

## 1. Configuración (MicroProfile Config / SmallRye)

### Fuentes y precedencia (mayor gana)
1. System properties: `java -Dexchange.spread=0.5 -jar ...`
2. Variables de entorno: `EXCHANGE_SPREAD=0.5` (punto y guiones → `_`, mayúsculas)
3. `.env` en el directorio de ejecución
4. `application.yml` / `application.properties`

> Esto es lo que hace viable Kubernetes: la misma imagen se configura por `ConfigMap`/`Secret` → variables de entorno, sin reconstruir.

### Perfiles
Equivalen a `spring.profiles`, pero en el mismo archivo con el prefijo `%`:

| Perfil | Cuándo se activa |
|---|---|
| `%dev` | `quarkus:dev` |
| `%test` | `mvn test` |
| `%prod` | `java -jar` / contenedor (por defecto) |
| custom | `-Dquarkus.profile=qa` o `QUARKUS_PROFILE=qa` |

```yaml
exchange:
  spread: 0.02          # base
"%dev":
  exchange:
    spread: 0.00        # solo en dev
```

### Valores por defecto y variables de entorno
```yaml
app:
  greeting-suffix: ${APP_SUFFIX:desde Yape}   # usa APP_SUFFIX si existe; si no, el default
```

### @ConfigProperty vs @ConfigMapping

| | `@ConfigProperty` | `@ConfigMapping` |
|---|---|---|
| Uso | Un valor suelto | Un grupo con prefijo |
| Spring | `@Value` | `@ConfigurationProperties` |
| Forma | Campo/parámetro | Interfaz tipada |
| Validación | Valor requerido | Estructura completa, anidada, `Map`, `List`, `Optional` |

```java
@ConfigMapping(prefix = "exchange")
public interface ExchangeConfig {
    String baseCurrency();             // exchange.base-currency
    BigDecimal spread();
    @WithDefault("config") String provider();
    Map<String, BigDecimal> rates();
}
```

**Falla rápido:** si falta una propiedad obligatoria, la app **no arranca**. Mejor que descubrirlo en producción a las 3 a.m.

### Seguridad en config
- Nunca secretos en `application.yml` versionado. Usa variables de entorno o `Secret` de K8s.
- `quarkus.config.profile.parent`, Vault o `config-yaml` + secret mounts son opciones avanzadas (módulo de seguridad).

## 2. CDI con ArC

Quarkus no usa Spring DI sino **CDI Lite** (Jakarta), implementado por **ArC**, que resuelve todo en build.

### Scopes

| Scope | Equivalente Spring | Notas |
|---|---|---|
| `@ApplicationScoped` | `@Service`/singleton | Lazy, con proxy cliente |
| `@Singleton` | singleton | Sin proxy, eager-ish |
| `@RequestScoped` | `@RequestScope` | Uno por request HTTP |
| `@Dependent` | `prototype` | Por defecto si no pones nada |

Regla práctica: **`@ApplicationScoped` por defecto** para servicios.

### Formas de inyectar
```java
@Inject Foo foo;               // campo (package-private, sin private)
Foo(Foo foo) { ... }           // constructor (recomendado, 1 solo constructor no requiere @Inject)
```

### Producers (≈ `@Bean`)
Para clases de terceros o que no controlas: `Clock`, clientes, `ObjectMapper`.
```java
@Produces @ApplicationScoped Clock clock() { return Clock.system(ZoneId.of("America/Lima")); }
```
Ventaja: en tests se reemplaza el reloj y el tiempo se vuelve determinista.

### Selección de beans en BUILD TIME
```java
@ApplicationScoped
@IfBuildProperty(name = "exchange.provider", stringValue = "fixed")
public class FixedRateProvider implements RateProvider { ... }
```
- Si la propiedad no coincide, **el bean no existe en el jar**. Es más ligero que `@ConditionalOnProperty`, pero **cambiarla exige reconstruir** (`-Dexchange.provider=fixed` en el `package`, no en `java -jar`).
- Alternativa en runtime: `@LookupIfProperty` + `Instance<T>`.

### Interceptores (≈ AOP `@Around`)
`@Audited` (binding) + `AuditInterceptor` (`@AroundInvoke`). Sirven para logging, métricas, auditoría. Sin proxies dinámicos por reflexión: ArC genera subclases en build.

## 3. Tests con configuración distinta
```java
@QuarkusTest
@TestProfile(NoSpread.class)       // perfil de test solo para esta clase
class ExchangeNoSpreadTest { ... }

public static class NoSpread implements QuarkusTestProfile {
    public Map<String,String> getConfigOverrides() { return Map.of("exchange.spread","0"); }
}
```
Cada `@TestProfile` distinto **reinicia la app** en tests: úsalo con moderación.

## 4. Verificado en esta clase

| Prueba | Resultado |
|---|---|
| `mvn test` (perfil test, spread 2%) | 100 PEN → 26.46 USD ✅ |
| `java -jar` (perfil prod, spread 1.5%) | 100 PEN → 26.60 USD ✅ |
| `-Dexchange.spread=0.5` | 100 PEN → 13.50 USD ✅ |
| `APP_SUFFIX="desde ENV"` | `m02-config desde ENV` ✅ |
| `-Dexchange.provider=fixed` (en build) | JPY pasa a "soportada" con tasa 1 ✅ |

## 5. Trampas comunes viniendo de Spring
- `@Value("${...}")` no existe: usa `@ConfigProperty(name="...")`.
- Con `@ConfigMapping` hace falta que sea **interfaz**, no clase con setters.
- Una propiedad `quarkus.*` marcada "fixed at build time" **no cambia** con `-D` en runtime (la Dev UI y la doc lo indican).
- Beans no inyectados en ningún lado pueden ser **eliminados** por ArC (unused bean removal). Si necesitas uno, anótalo con `@Unremovable`.

## 6. Preguntas de repaso
1. Si defino `exchange.spread` en `application.yml` y también `EXCHANGE_SPREAD` en el entorno, ¿cuál gana?
2. ¿Por qué `-Dexchange.provider=fixed` no funciona con `java -jar` sobre un jar ya construido?
3. ¿Cuándo usar `@ConfigMapping` en vez de `@ConfigProperty`?
4. ¿Qué ventaja tiene un `@Produces Clock` frente a llamar `Instant.now()`?

<details><summary>Respuestas</summary>

1. La variable de entorno.
2. Porque `@IfBuildProperty` se evalúa durante el build y descarta el bean no seleccionado del jar.
3. Cuando hay varias propiedades relacionadas con un prefijo, estructuras anidadas o mapas.
4. Se puede inyectar un reloj fijo en tests y obtener resultados deterministas.
</details>

## Siguiente
Clase 3: API REST completa con Hibernate ORM + Panache (CRUD de Beneficiarios Yape), validaciones, manejo de errores y OpenAPI.
