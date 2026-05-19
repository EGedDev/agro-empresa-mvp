package com.agroempresa.erp.finanzas.caja.dto;

import com.agroempresa.erp.finanzas.MetodoPago;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RegistrarCierreMetodoPagoRequest(
        @NotNull(message = "El metodo de pago es obligatorio")
        MetodoPago metodoPago,

        @NotNull(message = "El saldo reportado por metodo es obligatorio")
        @Digits(integer = 10, fraction = 2, message = "El saldo reportado por metodo debe tener como maximo 10 enteros y 2 decimales")
        BigDecimal saldoReportado
) {
}
