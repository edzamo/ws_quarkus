package pe.yape.training.hello;

import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * Recurso JAX-RS (Jakarta REST). Equivale a @RestController + @GetMapping.
 */
@Path("/hello")
@Produces(MediaType.TEXT_PLAIN)
public class GreetingResource {

    @Inject
    GreetingService service;

    @GET
    public String hello(@QueryParam("name") @DefaultValue("mundo") String name) {
        return service.greet(name);
    }
}
