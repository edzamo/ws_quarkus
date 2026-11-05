package pe.yape.training.beneficiarios.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** DTO de entrada. Las reglas de validación viven aquí, no en la entidad. */
public record BeneficiaryRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Pattern(regexp = "9\\d{8}", message = "debe ser un celular peruano de 9 dígitos que empieza con 9") String phone,
        @Size(max = 40) String alias,
        @Size(max=8) String documentNumber ){

}
