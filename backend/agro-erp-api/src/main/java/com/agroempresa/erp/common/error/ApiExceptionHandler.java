package com.agroempresa.erp.common.error;

import com.agroempresa.erp.common.tracing.RequestTraceContext;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse manejarRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        return error("NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse manejarReglaDeNegocio(BusinessException ex) {
        return error("BUSINESS_RULE_VIOLATION", ex.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse manejarConstraintViolations(ConstraintViolationException ex) {
        Map<String, String> errores = new LinkedHashMap<>();

        ex.getConstraintViolations().forEach(error ->
                errores.put(error.getPropertyPath().toString(), error.getMessage())
        );

        return validationError("La solicitud contiene datos invalidos", errores);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse manejarValidaciones(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errores.put(error.getField(), error.getDefaultMessage())
        );

        return validationError("La solicitud contiene datos invalidos", errores);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse manejarCuerpoInvalido(HttpMessageNotReadableException ex) {
        return error("MALFORMED_REQUEST", "El cuerpo de la solicitud no tiene un formato valido");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse manejarParametroInvalido(MethodArgumentTypeMismatchException ex) {
        return error("INVALID_PARAMETER", construirMensajeParametroInvalido(ex));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse manejarIntegridadDeDatos(DataIntegrityViolationException ex) {
        return error("DATA_INTEGRITY_VIOLATION", "La operacion viola una restriccion de datos");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse manejarIllegalArgument(IllegalArgumentException ex) {
        return error("BAD_REQUEST", ex.getMessage());
    }

    private ErrorResponse error(String codigo, String mensaje) {
        return new ErrorResponse(
                codigo,
                mensaje,
                RequestTraceContext.correlationIdActual(),
                Instant.now()
        );
    }

    private ValidationErrorResponse validationError(String mensaje, Map<String, String> errores) {
        return new ValidationErrorResponse(
                "VALIDATION_ERROR",
                mensaje,
                RequestTraceContext.correlationIdActual(),
                errores,
                Instant.now()
        );
    }

    private String construirMensajeParametroInvalido(MethodArgumentTypeMismatchException ex) {
        Class<?> tipoRequerido = ex.getRequiredType();

        if (tipoRequerido != null && tipoRequerido.isEnum()) {
            String valoresPermitidos = Arrays.stream(tipoRequerido.getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));

            return "El parametro '" + ex.getName() + "' debe ser uno de: " + valoresPermitidos;
        }

        return "El parametro '" + ex.getName() + "' tiene un valor invalido";
    }

    public record ErrorResponse(
            String code,
            String message,
            String correlationId,
            Instant timestamp
    ) {
    }

    public record ValidationErrorResponse(
            String code,
            String message,
            String correlationId,
            Map<String, String> errors,
            Instant timestamp
    ) {
    }
}
