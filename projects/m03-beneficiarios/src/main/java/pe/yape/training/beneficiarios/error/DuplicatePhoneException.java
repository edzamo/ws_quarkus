package pe.yape.training.beneficiarios.error;

public class DuplicatePhoneException extends RuntimeException {
    public DuplicatePhoneException(String phone) {
        super("Ya existe un beneficiario con el celular " + phone);
    }
}
