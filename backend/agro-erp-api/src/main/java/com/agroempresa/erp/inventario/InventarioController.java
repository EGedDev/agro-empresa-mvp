package com.agroempresa.erp.inventario;

import com.agroempresa.erp.inventario.dto.MovimientoInventarioResponse;
import com.agroempresa.erp.inventario.dto.RegistrarMovimientoInventarioRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventario/movimientos")
@Validated
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @GetMapping
    public List<MovimientoInventarioResponse> listarUltimosMovimientos() {
        return inventarioService.listarUltimosMovimientos();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovimientoInventarioResponse registrarMovimientoManual(
            @Valid @RequestBody RegistrarMovimientoInventarioRequest request
    ) {
        return inventarioService.registrarMovimientoManual(request);
    }

    @GetMapping("/producto/{productoId}")
    public List<MovimientoInventarioResponse> listarMovimientosPorProducto(
            @PathVariable @Positive(message = "El id del producto debe ser mayor a cero") Long productoId
    ) {
        return inventarioService.listarMovimientosPorProducto(productoId);
    }
}
