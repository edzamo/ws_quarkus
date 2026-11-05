package pe.yape.training.beneficiarios.error;

import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Traduce excepciones de dominio a respuestas HTTP (≈ @RestControllerAdvice + @ExceptionHandler).
 * @ServerExceptionMapper es la forma corta de Quarkus REST; el ExceptionMapper de JAX-RS también funciona.
 */
public class ErrorMappers {

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> notFound(BeneficiaryNotFoundException e) {
        return RestResponse.status(RestResponse.Status.NOT_FOUND, ErrorResponse.of("NOT_FOUND", e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> duplicate(DuplicatePhoneException e) {
        return RestResponse.status(RestResponse.Status.CONFLICT, ErrorResponse.of("DUPLICATE_PHONE", e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> validation(ConstraintViolationException e) {
        String detail = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath().toString().replaceAll("^.*\\.", "") + ": " + v.getMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return RestResponse.status(RestResponse.Status.BAD_REQUEST, ErrorResponse.of("VALIDATION_ERROR", detail));
    }
    @ServerExceptionMapper
    public RestResponse<ErrorResponse> duplicateDocumentNumber(DuplicateDocumentNumberException e) {
          return RestResponse.status(RestResponse.Status.CONFLICT, ErrorResponse.of("DUPLICATE_DOCUMENT_NUMBER", e.getMessage()));
      }
}
