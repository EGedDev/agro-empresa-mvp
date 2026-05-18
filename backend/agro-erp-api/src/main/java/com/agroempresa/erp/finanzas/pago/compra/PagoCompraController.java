package com.agroempresa.erp.finanzas.pago.compra;

import com.agroempresa.erp.common.pagination.PaginaResponse;
import com.agroempresa.erp.finanzas.MetodoPago;
import com.agroempresa.erp.finanzas.pago.compra.dto.PagoCompraResponse;
import com.agroempresa.erp.finanzas.pago.compra.dto.RegistrarPagoCompraRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/compras/{compraId}/pagos")
@Validated
public class PagoCompraController {

    private final PagoCompraService pagoCompraService;

    public PagoCompraController(PagoCompraService pagoCompraService) {
        this.pagoCompraService = pagoCompraService;
    }

    @GetMapping
    public PaginaResponse<PagoCompraResponse> listarPorCompra(
            @PathVariable @Positive(message = "El id de la compra debe ser mayor a cero") Long compraId,
            @RequestParam(required = false) MetodoPago metodoPago,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return pagoCompraService.listarPorCompra(compraId, metodoPago, desde, hasta, page, size, sort);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PagoCompraResponse registrar(
            @PathVariable @Positive(message = "El id de la compra debe ser mayor a cero") Long compraId,
            @Valid @RequestBody RegistrarPagoCompraRequest request
    ) {
        return pagoCompraService.registrar(compraId, request);
    }
}
