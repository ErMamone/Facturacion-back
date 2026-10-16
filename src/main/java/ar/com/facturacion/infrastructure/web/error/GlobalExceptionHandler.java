package ar.com.facturacion.infrastructure.web.error;

import ar.com.facturacion.domain.exception.FacturaNoEncontradaException;
import ar.com.facturacion.domain.exception.ReglaDeNegocioException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;

/**
 * Traduce excepciones a {@link ProblemDetail} (RFC 9457). Los errores de validación agregan
 * {@code errors: [{campo, mensaje}]} para que el frontend los muestre junto a cada input.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    public record ErrorCampo(String campo, String mensaje) {
    }

    @ExceptionHandler(FacturaNoEncontradaException.class)
    ProblemDetail noEncontrada(FacturaNoEncontradaException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Factura no encontrada");
        return problem;
    }

    @ExceptionHandler(ReglaDeNegocioException.class)
    ProblemDetail reglaDeNegocio(ReglaDeNegocioException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Solicitud inválida");
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        List<ErrorCampo> errores = new ArrayList<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errores.add(new ErrorCampo(e.getField(), e.getDefaultMessage())));
        ex.getBindingResult().getGlobalErrors()
                .forEach(e -> errores.add(new ErrorCampo(e.getObjectName(), e.getDefaultMessage())));
        return validacion(ex.getBody(), errores, headers, status, ex, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        List<ErrorCampo> errores = new ArrayList<>();
        ex.getParameterValidationResults().forEach(r -> r.getResolvableErrors().forEach(e -> {
            String campo = e instanceof FieldError fe ? fe.getField() : r.getMethodParameter().getParameterName();
            errores.add(new ErrorCampo(campo, e.getDefaultMessage()));
        }));
        return validacion(ex.getBody(), errores, headers, status, ex, request);
    }

    /** JSON ilegible o con un valor del tipo equivocado (p. ej. moneda "EUR", cantidad 1.5): se indica el campo. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(status, "El cuerpo no es un JSON válido");
        problem.setTitle("Solicitud inválida");
        if (ex.getMostSpecificCause() instanceof JacksonException je && !je.getPath().isEmpty()) {
            String campo = ruta(je.getPath());
            problem.setDetail("Valor inválido en '" + campo + "'");
            problem.setProperty("errors", List.of(new ErrorCampo(campo, "valor inválido o de tipo incorrecto")));
        }
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    /** [vendedor, cuit] -> "vendedor.cuit"; [items, 0, cantidad] -> "items[0].cantidad" */
    private static String ruta(List<JacksonException.Reference> path) {
        var sb = new StringBuilder();
        for (var ref : path) {
            if (ref.getPropertyName() != null) {
                sb.append(sb.isEmpty() ? "" : ".").append(ref.getPropertyName());
            } else if (ref.getIndex() >= 0) {
                sb.append('[').append(ref.getIndex()).append(']');
            }
        }
        return sb.toString();
    }

    private ResponseEntity<Object> validacion(ProblemDetail problem, List<ErrorCampo> errores, HttpHeaders headers,
                                              HttpStatusCode status, Exception ex, WebRequest request) {
        problem.setTitle("Datos inválidos");
        problem.setDetail("La solicitud tiene errores de validación");
        problem.setProperty("errors", errores);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }
}
