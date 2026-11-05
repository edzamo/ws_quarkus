package pe.yape.training.product;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/** Cliente REST declarativo y reactivo (≈ OpenFeign / WebClient). Se implementa en build time. */
@RegisterRestClient(configKey = "customer")
@Path("/customers")
public interface CustomerClient {

    @GET
    @Path("/{id}")
    Uni<Customer> get(@PathParam("id") Long id, @QueryParam("delayMs") long delayMs);
}
