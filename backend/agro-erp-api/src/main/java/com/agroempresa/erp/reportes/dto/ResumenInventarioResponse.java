package com.agroempresa.erp.reportes.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ResumenInventarioResponse(
        LocalDate desde,
        LocalDate hasta,
        long productosActivos,
        long productosConStockBajo,
        ResumenMovimientosInventario entradas,
        ResumenMovimientosInventario salidas,
        long unidadesNetas,
        LocalDateTime generadoEn
) {
}
