package com.agroempresa.erp.finanzas.pago.venta;

import com.agroempresa.erp.common.pagination.PaginaResponse;
import com.agroempresa.erp.finanzas.MetodoPago;
import com.agroempresa.erp.finanzas.pago.venta.dto.PagoVentaResponse;
import com.agroempresa.erp.finanzas.pago.venta.dto.RegistrarPagoVentaRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/ventas/{ventaId}/pagos")
@Validated
public class PagoVentaController {

    private final PagoVentaService pagoVentaService;

    public PagoVentaController(PagoVentaService pagoVentaService) {
        this.pagoVentaService = pagoVentaService;
    }

    @GetMapping
    public PaginaResponse<PagoVentaResponse> listarPorVenta(
            @PathVariable @Positive(message = "El id de la venta debe ser mayor a cero") Long ventaId,
            @RequestParam(required = false) MetodoPago metodoPago,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return pagoVentaService.listarPorVenta(ventaId, metodoPago, desde, hasta, page, size, sort);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PagoVentaResponse registrar(
            @PathVariable @Positive(message = "El id de la venta debe ser mayor a cero") Long ventaId,
            @Valid @RequestBody RegistrarPagoVentaRequest request
    ) {
        return pagoVentaService.registrar(ventaId, request);
    }
}
