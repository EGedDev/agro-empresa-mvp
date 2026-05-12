package com.agroempresa.erp.common.error;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse manejarRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        return new ErrorResponse(
                "NOT_FOUND",
                ex.getMessage(),
                Instant.now()
        );
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse manejarReglaDeNegocio(BusinessException ex) {
        return new ErrorResponse(
                "BUSINESS_RULE_VIOLATION",
                ex.getMessage(),
                Instant.now()
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse manejarConstraintViolations(ConstraintViolationException ex) {
        Map<String, String> errores = new LinkedHashMap<>();

        ex.getConstraintViolations().forEach(error ->
                errores.put(error.getPropertyPath().toString(), error.getMessage())
        );

        return new ValidationErrorResponse(
                "VALIDATION_ERROR",
                "La solicitud contiene datos inválidos",
                errores,
                Instant.now()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse manejarValidaciones(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errores.put(error.getField(), error.getDefaultMessage())
        );

        return new ValidationErrorResponse(
                "VALIDATION_ERROR",
                "La solicitud contiene datos inválidos",
                errores,
                Instant.now()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse manejarCuerpoInvalido(HttpMessageNotReadableException ex) {
        return new ErrorResponse(
                "MALFORMED_REQUEST",
                "El cuerpo de la solicitud no tiene un formato válido",
                Instant.now()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse manejarIllegalArgument(IllegalArgumentException ex) {
        return new ErrorResponse(
                "BAD_REQUEST",
                ex.getMessage(),
                Instant.now()
        );
    }

    public record ErrorResponse(
            String code,
            String message,
            Instant timestamp
    ) {
    }

    public record ValidationErrorResponse(
            String code,
            String message,
            Map<String, String> errors,
            Instant timestamp
    ) {
    }
}
