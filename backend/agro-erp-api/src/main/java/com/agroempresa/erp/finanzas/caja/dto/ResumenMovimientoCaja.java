package com.agroempresa.erp.finanzas.caja.dto;

import java.math.BigDecimal;

public record ResumenMovimientoCaja(
        long cantidadMovimientos,
        BigDecimal total
) {
}
