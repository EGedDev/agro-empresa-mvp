package com.agroempresa.erp.finanzas.caja.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RegistrarCierreCajaRequest(
        @NotNull(message = "La fecha inicial es obligatoria")
        LocalDate desde,

        @NotNull(message = "La fecha final es obligatoria")
        LocalDate hasta,

        @NotNull(message = "El saldo reportado es obligatorio")
        @Digits(integer = 10, fraction = 2, message = "El saldo reportado debe tener como maximo 10 enteros y 2 decimales")
        BigDecimal saldoReportado,

        @Size(max = 500, message = "Las observaciones no deben superar los 500 caracteres")
        String observaciones,

        @Valid
        @Size(max = 6, message = "No se pueden registrar mas metodos de pago que los soportados")
        List<RegistrarCierreMetodoPagoRequest> metodos
) {
}
