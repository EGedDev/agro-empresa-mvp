package com.agroempresa.erp.finanzas.caja.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ResumenCajaPorMetodoPagoResponse(
        LocalDate desde,
        LocalDate hasta,
        List<ResumenMetodoPagoCajaResponse> metodos,
        BigDecimal saldoNeto,
        LocalDateTime generadoEn
) {
}
