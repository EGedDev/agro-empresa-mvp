package com.agroempresa.erp.comercial.venta.dto;

import java.util.List;

public record VentaRequest(
        Long clienteId,
        List<VentaDetalleRequest> detalles
) {
}