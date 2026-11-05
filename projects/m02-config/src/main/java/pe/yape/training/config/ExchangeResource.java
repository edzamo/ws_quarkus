package pe.yape.training.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.math.BigDecimal;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/exchange")
@Produces(MediaType.APPLICATION_JSON)
public class ExchangeResource {

    private final ExchangeService service;

    // @ConfigProperty: para valores sueltos. Para grupos usa @ConfigMapping.
    @ConfigProperty(name = "app.name")
    String appName;

    @ConfigProperty(name = "app.greeting-suffix")
    String suffix;

    ExchangeResource(ExchangeService service) {
        this.service = service;
    }

    @GET
    @Path("/convert")
    public Conversion convert(@QueryParam("to") @NotNull String to,
                              @QueryParam("amount") @NotNull @DecimalMin("0.01") BigDecimal amount) {
        return service.convert(to, amount);
    }

    @GET
    @Path("/info")
    public String info() {
        return appName + " " + suffix;
    }
}
