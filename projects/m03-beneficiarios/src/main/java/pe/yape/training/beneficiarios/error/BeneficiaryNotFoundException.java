package pe.yape.training.beneficiarios.error;

public class BeneficiaryNotFoundException extends RuntimeException {
    public BeneficiaryNotFoundException(Long id) {
        super("Beneficiario no encontrado: " + id);
    }
}
