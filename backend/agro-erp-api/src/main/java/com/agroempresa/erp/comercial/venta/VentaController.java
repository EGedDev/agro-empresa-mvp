package com.agroempresa.erp.comercial.venta;

import com.agroempresa.erp.comercial.venta.dto.VentaRequest;
import com.agroempresa.erp.comercial.venta.dto.VentaResponse;
import com.agroempresa.erp.common.pagination.PaginaResponse;
import com.agroempresa.erp.finanzas.EstadoPago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/ventas")
@Validated
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    @GetMapping
    public PaginaResponse<VentaResponse> listar(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) EstadoVenta estado,
            @RequestParam(required = false) EstadoPago estadoPago,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return ventaService.listar(clienteId, estado, estadoPago, desde, hasta, page, size, sort);
    }

    @GetMapping("/cliente/{clienteId}")
    public PaginaResponse<VentaResponse> listarPorCliente(
            @PathVariable @Positive(message = "El id del cliente debe ser mayor a cero") Long clienteId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return ventaService.listarPorCliente(clienteId, page, size, sort);
    }

    @GetMapping("/estado/{estado}")
    public PaginaResponse<VentaResponse> listarPorEstado(
            @PathVariable EstadoVenta estado,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return ventaService.listarPorEstado(estado, page, size, sort);
    }

    @GetMapping("/{id}")
    public VentaResponse obtenerPorId(@PathVariable @Positive(message = "El id debe ser mayor a cero") Long id) {
        return ventaService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VentaResponse crear(@Valid @RequestBody VentaRequest request) {
        return ventaService.crear(request);
    }

    @PatchMapping("/{id}/cancelar")
    public VentaResponse cancelar(@PathVariable @Positive(message = "El id debe ser mayor a cero") Long id) {
        return ventaService.cancelar(id);
    }
}
