package com.agroempresa.erp.finanzas.pago.venta.dto;

import com.agroempresa.erp.finanzas.MetodoPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RegistrarPagoVentaRequest(
        @NotNull(message = "El monto del pago es obligatorio")
        @DecimalMin(value = "0.01", inclusive = true, message = "El monto del pago debe ser mayor a cero")
        @Digits(integer = 10, fraction = 2, message = "El monto del pago debe tener como máximo 10 enteros y 2 decimales")
        BigDecimal monto,

        @NotNull(message = "El método de pago es obligatorio")
        MetodoPago metodoPago,

        @Size(max = 120, message = "La referencia no debe superar los 120 caracteres")
        String referencia
) {
}
