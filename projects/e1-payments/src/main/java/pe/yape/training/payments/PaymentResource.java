package pe.yape.training.payments;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@Path("/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PaymentResource {

    @Channel("payments-out")
    Emitter<PagoRealizado> emitter;

    PaymentConsumer consumer;

    PaymentResource(PaymentConsumer consumer) {
        this.consumer = consumer;
    }

    /** Publica el evento y responde 202: el procesamiento es asíncrono, no hay resultado aún. */
    @POST
    public Uni<Response> publish(PagoRealizado pago) {
        return Uni.createFrom().completionStage(emitter.send(pago))
                .replaceWith(Response.accepted(pago).build());
    }

    @GET
    @Path("/processed")
    public List<String> processed() {
        return consumer.processedIds();
    }
}
