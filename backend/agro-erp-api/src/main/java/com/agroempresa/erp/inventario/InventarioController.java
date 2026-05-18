package com.agroempresa.erp.inventario;

import com.agroempresa.erp.common.pagination.PaginaResponse;
import com.agroempresa.erp.inventario.dto.MovimientoInventarioResponse;
import com.agroempresa.erp.inventario.dto.RegistrarMovimientoInventarioRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/inventario/movimientos")
@Validated
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @GetMapping
    public PaginaResponse<MovimientoInventarioResponse> listarMovimientos(
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) TipoMovimientoInventario tipo,
            @RequestParam(required = false) String referenciaTipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return inventarioService.listarMovimientos(productoId, tipo, referenciaTipo, desde, hasta, page, size, sort);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovimientoInventarioResponse registrarMovimientoManual(
            @Valid @RequestBody RegistrarMovimientoInventarioRequest request
    ) {
        return inventarioService.registrarMovimientoManual(request);
    }

    @GetMapping("/producto/{productoId}")
    public PaginaResponse<MovimientoInventarioResponse> listarMovimientosPorProducto(
            @PathVariable @Positive(message = "El id del producto debe ser mayor a cero") Long productoId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return inventarioService.listarMovimientosPorProducto(productoId, page, size, sort);
    }
}
