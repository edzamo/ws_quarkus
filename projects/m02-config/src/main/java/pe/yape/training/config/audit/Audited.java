package pe.yape.training.config.audit;

import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.*;

/** Interceptor binding: marca los métodos a auditar (≈ un aspecto @Around de Spring AOP). */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Audited {
}
