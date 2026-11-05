package pe.yape.training.beneficiarios.error;

public class BeneficiaryPhoneNotFoundException extends RuntimeException {

  public BeneficiaryPhoneNotFoundException(String phone) {
    super("Beneficiario no encontrado con el celular: " + phone);
  }
}
