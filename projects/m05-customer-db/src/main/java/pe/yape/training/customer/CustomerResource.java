package pe.yape.training.customer;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

/**
 * CRUD reactivo sobre PostgreSQL. Dos reglas nuevas respecto a la clase 3:
 *  - Las escrituras necesitan una transacción REACTIVA: Panache.withTransaction(...).
 *  - Las lecturas simples no la necesitan (Hibernate Reactive abre sesión por operación).
 */
@Path("/customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomerResource {

    @GET
    public Uni<List<CustomerDto>> list() {
        return Customer.<Customer>listAll(Sort.by("id"))
                .map(list -> list.stream().map(CustomerDto::from).toList());
    }

    @GET
    @Path("/{id}")
    public Uni<CustomerDto> get(@PathParam("id") Long id) {
        return Customer.<Customer>findById(id)
                .onItem().ifNull().failWith(() -> notFound(id))
                .map(CustomerDto::from);
    }

    @GET
    @Path("/by-tier/{tier}")
    public Uni<List<CustomerDto>> listByTier(@PathParam("tier") String tier) {
        return Customer.<Customer>list("tier", tier)
                .map(list -> list.stream().map(CustomerDto::from).toList());
    }

    @POST
    public Uni<Response> create(@Valid CustomerDto dto) {
        Customer c = new Customer();
        c.name = dto.name();
        c.tier = dto.tier() == null ? "BASIC" : dto.tier();
        c.phone= dto.phone();
        return Panache.withTransaction(c::persist)
                .map(x -> Response.created(URI.create("/customers/" + c.id))
                        .entity(CustomerDto.from(c)).build());
    }

    @PUT
    @Path("/{id}")
    public Uni<CustomerDto> update(@PathParam("id") Long id, @Valid CustomerDto dto) {
        // La entidad se carga DENTRO de la transacción: el cambio se detecta (dirty checking) y se hace UPDATE al commit.
        return Panache.withTransaction(() -> Customer.<Customer>findById(id)
                        .onItem().ifNull().failWith(() -> notFound(id))
                        .invoke(c -> {
                            c.name = dto.name();
                            if (dto.tier() != null) {
                                c.tier = dto.tier();
                            }
                        }))
                .map(CustomerDto::from);
    }

    @DELETE
    @Path("/{id}")
    public Uni<Response> delete(@PathParam("id") Long id) {
        return Panache.withTransaction(() -> Customer.deleteById(id))
                .map(deleted -> deleted ? Response.noContent().build()
                        : Response.status(Response.Status.NOT_FOUND).build());
    }

    private static NotFoundException notFound(Long id) {
        return new NotFoundException("Cliente no encontrado: " + id);
    }
}
