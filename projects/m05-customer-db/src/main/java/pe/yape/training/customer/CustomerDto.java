package pe.yape.training.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Contrato público: no exponemos la entidad. `id` solo sale en la respuesta. */
public record CustomerDto(Long id,
                          @NotBlank String name,
                          @Pattern(regexp = "BASIC|GOLD", message = "tier debe ser BASIC o GOLD") String tier,
                          String phone) {

    static CustomerDto from(Customer c) {
        return new CustomerDto(c.id, c.name, c.tier, c.phone);
    }
}
