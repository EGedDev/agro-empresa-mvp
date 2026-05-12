package com.agroempresa.erp.inventario;

import com.agroempresa.erp.inventario.dto.MovimientoInventarioResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventario/movimientos")
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @GetMapping
    public List<MovimientoInventarioResponse> listarUltimosMovimientos() {
        return inventarioService.listarUltimosMovimientos();
    }

    @GetMapping("/producto/{productoId}")
    public List<MovimientoInventarioResponse> listarMovimientosPorProducto(@PathVariable Long productoId) {
        return inventarioService.listarMovimientosPorProducto(productoId);
    }
}