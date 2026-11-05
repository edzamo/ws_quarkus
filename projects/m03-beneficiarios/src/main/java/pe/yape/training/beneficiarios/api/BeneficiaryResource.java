package pe.yape.training.beneficiarios.api;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.yape.training.beneficiarios.domain.Beneficiary;
import pe.yape.training.beneficiarios.domain.BeneficiaryService;

@Path("/beneficiaries")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Beneficiarios", description = "Gestión de beneficiarios Yape")
public class BeneficiaryResource {

  private static final Logger log = LoggerFactory.getLogger(BeneficiaryResource.class);
  private final BeneficiaryService service;

    BeneficiaryResource(BeneficiaryService service) {
        this.service = service;
    }

    @GET
    @Operation(summary = "Lista beneficiarios con búsqueda por nombre y paginación")
    public List<BeneficiaryResponse> list(@QueryParam("q") String q,
                                          @QueryParam("page") @DefaultValue("0") int page,
                                          @QueryParam("size") @DefaultValue("20") int size) {
        return service.list(q, page, Math.min(size, 100)).stream().map(BeneficiaryResponse::from).toList();
    }

    @GET
    @Path("/{id}")
    public BeneficiaryResponse get(@PathParam("id") Long id) {
        return BeneficiaryResponse.from(service.get(id));
    }

    @GET
    @Path("/by-phone/{phone}")
    public BeneficiaryResponse getByPhone(@PathParam("phone") String phone) {
      return BeneficiaryResponse.from(service.getByPhone(phone));
    }

    @POST
    @Operation(summary = "Crea un beneficiario")
    public RestResponse<BeneficiaryResponse> create(@Valid BeneficiaryRequest request, UriInfo uriInfo) {
       log.info("Creating beneficiary with request: {}", request);
        Beneficiary created = service.create(request);
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(created.id)).build();
        return RestResponse.ResponseBuilder.<BeneficiaryResponse>created(location)
                .entity(BeneficiaryResponse.from(created))
                .build();
    }

    @PUT
    @Path("/{id}")
    public BeneficiaryResponse update(@PathParam("id") Long id, @Valid BeneficiaryRequest request) {
        return BeneficiaryResponse.from(service.update(id, request));
    }

    @DELETE
    @Path("/{id}")
    public RestResponse<Void> delete(@PathParam("id") Long id) {
        service.delete(id);
        return RestResponse.noContent();
    }

}
