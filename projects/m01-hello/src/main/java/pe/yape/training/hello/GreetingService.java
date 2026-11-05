package pe.yape.training.hello;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Bean CDI. @ApplicationScoped = singleton (equivale a @Service de Spring).
 * ArC (el contenedor CDI de Quarkus) lo resuelve en BUILD TIME, no en runtime.
 */
@ApplicationScoped
public class GreetingService {

    public String greet(String name) {
        return "Hola " + name + ", bienvenido a Quarkus en Yape!!";
    }
}
