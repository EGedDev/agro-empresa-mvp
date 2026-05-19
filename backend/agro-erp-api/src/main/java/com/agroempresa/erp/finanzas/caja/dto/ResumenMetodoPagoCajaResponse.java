package com.agroempresa.erp.finanzas.caja.dto;

import com.agroempresa.erp.finanzas.MetodoPago;

import java.math.BigDecimal;

public record ResumenMetodoPagoCajaResponse(
        MetodoPago metodoPago,
        ResumenMovimientoCaja ingresos,
        ResumenMovimientoCaja egresos,
        BigDecimal saldoNeto
) {
}
