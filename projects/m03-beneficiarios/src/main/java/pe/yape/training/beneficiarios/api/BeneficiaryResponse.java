package pe.yape.training.beneficiarios.api;

import java.time.Instant;
import pe.yape.training.beneficiarios.domain.Beneficiary;

/** DTO de salida: el contrato público de la API, desacoplado de la entidad. */
public record BeneficiaryResponse(Long id, String fullName, String phone, String alias, Instant createdAt) {

    public static BeneficiaryResponse from(Beneficiary b) {
        return new BeneficiaryResponse(b.id, b.fullName, b.phone, b.alias, b.createdAt);
    }
}
