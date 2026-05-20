package com.agroempresa.erp.comercial.compra;

import com.agroempresa.erp.comercial.compra.dto.CompraRequest;
import com.agroempresa.erp.comercial.compra.dto.CompraResponse;
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
@RequestMapping("/api/v1/compras")
@Validated
public class CompraController {

    private final CompraService compraService;

    public CompraController(CompraService compraService) {
        this.compraService = compraService;
    }

    @GetMapping
    public PaginaResponse<CompraResponse> listar(
            @RequestParam(required = false) String numero,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) EstadoCompra estado,
            @RequestParam(required = false) EstadoPago estadoPago,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return compraService.listar(numero, proveedorId, estado, estadoPago, desde, hasta, page, size, sort);
    }

    @GetMapping("/proveedor/{proveedorId}")
    public PaginaResponse<CompraResponse> listarPorProveedor(
            @PathVariable @Positive(message = "El id del proveedor debe ser mayor a cero") Long proveedorId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return compraService.listarPorProveedor(proveedorId, page, size, sort);
    }

    @GetMapping("/estado/{estado}")
    public PaginaResponse<CompraResponse> listarPorEstado(
            @PathVariable EstadoCompra estado,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return compraService.listarPorEstado(estado, page, size, sort);
    }

    @GetMapping("/{id}")
    public CompraResponse obtenerPorId(@PathVariable @Positive(message = "El id debe ser mayor a cero") Long id) {
        return compraService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompraResponse crear(@Valid @RequestBody CompraRequest request) {
        return compraService.crear(request);
    }

    @PatchMapping("/{id}/cancelar")
    public CompraResponse cancelar(@PathVariable @Positive(message = "El id debe ser mayor a cero") Long id) {
        return compraService.cancelar(id);
    }
}
