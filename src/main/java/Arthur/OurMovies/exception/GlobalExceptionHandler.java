package Arthur.OurMovies.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    public record Erro(int status, String mensagem, List<String> detalhes) {}
    @ExceptionHandler(ApiException.class)
    ResponseEntity<Erro> api(ApiException ex) { return response(ex.getStatus(), ex.getMessage(), List.of()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Erro> validation(MethodArgumentNotValidException ex) {
        return response(HttpStatus.BAD_REQUEST, "Dados inválidos.", ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage()).toList());
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, HandlerMethodValidationException.class, ConstraintViolationException.class})
    ResponseEntity<Erro> invalid(Exception ex) {
        return response(HttpStatus.BAD_REQUEST, "Requisição inválida. Verifique os campos, parâmetros e tipos informados.", List.of());
    }
    @ExceptionHandler({DataIntegrityViolationException.class, PessimisticLockingFailureException.class})
    ResponseEntity<Erro> conflict(Exception ex) {
        return response(HttpStatus.CONFLICT, "Operação em conflito com os dados existentes ou com outra atualização.", List.of());
    }
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ResponseEntity<Erro> uploadGrande(Exception ex) {
        return response(HttpStatus.PAYLOAD_TOO_LARGE, "A foto deve ter no maximo 5 MB.", List.of());
    }
    @ExceptionHandler({org.springframework.web.multipart.support.MissingServletRequestPartException.class,
            org.springframework.web.multipart.MultipartException.class})
    ResponseEntity<Erro> multipart(Exception ex) {
        return response(HttpStatus.BAD_REQUEST, "Envie a foto no campo multipart foto.", List.of());
    }
    private ResponseEntity<Erro> response(HttpStatus status, String message, List<String> details) {
        return ResponseEntity.status(status).body(new Erro(status.value(), message, details));
    }
}
