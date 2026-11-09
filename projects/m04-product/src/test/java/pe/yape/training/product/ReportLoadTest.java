package pe.yape.training.product;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

import io.quarkus.test.junit.QuarkusTest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Reto 3 — mide cómo se degrada /products/1 (rápido, en memoria) mientras una llamada a
 * /report bloquea el event loop con Thread.sleep(2000).
 *
 * El perfil de test fija quarkus.vertx.event-loops-pool-size=1 (ver application.yml) para
 * que el experimento sea determinista: con un solo hilo de event loop, bloquearlo bloquea
 * literalmente TODO lo demás, sin depender de cuántos núcleos tenga la máquina que corre
 * el test.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReportLoadTest {

    private static final int PETICIONES_CONCURRENTES = 20;

    @Test
    void endpointsDeReportResponden200() {
        given().when().get("/products/1/report").then().statusCode(200);
        given().when().get("/products/1/report/blocking").then().statusCode(200)
                .body("thread", org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("eventloop")));
        given().when().get("/products/1/report/virtual-thread").then().statusCode(200);
        given().when().get("/products/1/report/worker-pool").then().statusCode(200)
                .body("thread", org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("eventloop")));
    }

    @Test
    void casoRotoDegradaFuertementeLaLatenciaDeProductos1() throws Exception {
        Resultado r = medir("/products/1/report");
        imprimirFila("ROTO (sin arreglo)", r);

        // Con 1 solo event loop bloqueado 2s, las 20 peticiones concurrentes a /products/1
        // deben esperar esos ~2s (siguen la cola del único hilo).
        assertThat((long) r.p50, is(org.hamcrest.Matchers.greaterThan(1000L)));
    }

    @Test
    void blockingArreglaLaDegradacion() throws Exception {
        Resultado r = medir("/products/1/report/blocking");
        imprimirFila("(a) @Blocking", r);

        assertThat((long) r.p99, is(lessThan(500L)));
    }

    @Test
    void virtualThreadArreglaLaDegradacion() throws Exception {
        Resultado r = medir("/products/1/report/virtual-thread");
        imprimirFila("(b) @RunOnVirtualThread", r);

        assertThat((long) r.p99, is(lessThan(500L)));
    }

    @Test
    void workerPoolArreglaLaDegradacion() throws Exception {
        Resultado r = medir("/products/1/report/worker-pool");
        imprimirFila("(c) runSubscriptionOn(workerPool)", r);

        assertThat((long) r.p99, is(lessThan(500L)));
    }

    /**
     * Dispara 1 llamada al endpoint de report (que tarda 2s) y, al mismo tiempo,
     * PETICIONES_CONCURRENTES llamadas a /products/1. Devuelve p50/p99 (ms) de esas
     * llamadas rápidas.
     */
    private Resultado medir(String reportPath) throws Exception {
        // Warm-up: la primera vez que se usa un mecanismo (p.ej. el executor de hilos
        // virtuales) tiene costo de inicialización en toda la JVM; no queremos medir eso.
        given().when().get(reportPath).then().statusCode(200);

        ExecutorService pool = Executors.newFixedThreadPool(PETICIONES_CONCURRENTES + 1);
        try {
            Future<Long> reportFuture = pool.submit(() -> {
                given().when().get(reportPath).then().statusCode(200);
                return 0L;
            });
            // Pequeña ventaja de salida: aseguramos que el bloqueo (si lo hay) ya haya
            // empezado en el event loop antes de que llegue la ráfaga de tráfico.
            Thread.sleep(100);

            List<Future<Long>> productFutures = new ArrayList<>();
            for (int i = 0; i < PETICIONES_CONCURRENTES; i++) {
                productFutures.add(pool.submit(medirUnaLlamadaAProducto()));
            }

            List<Long> latenciasMs = new ArrayList<>();
            for (Future<Long> f : productFutures) {
                latenciasMs.add(f.get());
            }
            reportFuture.get();

            Collections.sort(latenciasMs);
            return new Resultado(percentil(latenciasMs, 50), percentil(latenciasMs, 99));
        } finally {
            pool.shutdown();
        }
    }

    private static Callable<Long> medirUnaLlamadaAProducto() {
        return () -> {
            long start = System.nanoTime();
            given().when().get("/products/1").then().statusCode(200);
            return (System.nanoTime() - start) / 1_000_000;
        };
    }

    private static long percentil(List<Long> ordenada, int p) {
        int n = ordenada.size();
        int idx = Math.max(0, Math.min(n - 1, (int) Math.ceil(p / 100.0 * n) - 1));
        return ordenada.get(idx);
    }

    private static void imprimirFila(String escenario, Resultado r) {
        System.out.printf("REPORTE-RETO3 | %-32s | p50=%4dms | p99=%4dms%n", escenario, r.p50, r.p99);
    }

    private record Resultado(long p50, long p99) {
    }
}
