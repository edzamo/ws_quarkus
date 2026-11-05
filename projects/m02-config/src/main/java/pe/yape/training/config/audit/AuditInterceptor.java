package pe.yape.training.config.audit;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.jboss.logging.Logger;

@Audited
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class AuditInterceptor {

    private static final Logger LOG = Logger.getLogger(AuditInterceptor.class);

    @AroundInvoke
    Object audit(InvocationContext ctx) throws Exception {
        long start = System.nanoTime();
        try {
            return ctx.proceed();
        } finally {
            LOG.debugf("[AUDIT] %s.%s took %d µs", ctx.getTarget().getClass().getSimpleName(),
                    ctx.getMethod().getName(), (System.nanoTime() - start) / 1_000);
        }
    }
}
