package com.agroempresa.erp.common.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
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

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse manejarIllegalArgument(IllegalArgumentException ex) {
        return new ErrorResponse(
                "BAD_REQUEST",
                ex.getMessage(),
                Instant.now()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse manejarValidaciones(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();

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