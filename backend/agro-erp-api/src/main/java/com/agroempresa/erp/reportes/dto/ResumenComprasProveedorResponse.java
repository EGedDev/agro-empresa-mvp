package com.agroempresa.erp.reportes.dto;

import java.math.BigDecimal;

public record ResumenComprasProveedorResponse(
        Long proveedorId,
        String proveedorNombre,
        Long compras,
        BigDecimal total,
        BigDecimal saldoPendiente
) {
}
