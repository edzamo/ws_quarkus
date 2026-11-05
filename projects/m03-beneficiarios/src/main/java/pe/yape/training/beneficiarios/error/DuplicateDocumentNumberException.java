package pe.yape.training.beneficiarios.error;

public class DuplicateDocumentNumberException extends RuntimeException {
    public DuplicateDocumentNumberException(String phone) {
        super("Ya existe un beneficiario con el celular " + phone);
    }
}
