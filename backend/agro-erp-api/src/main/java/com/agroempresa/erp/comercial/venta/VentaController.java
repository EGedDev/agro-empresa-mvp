package com.agroempresa.erp.comercial.venta;

import com.agroempresa.erp.comercial.venta.dto.VentaRequest;
import com.agroempresa.erp.comercial.venta.dto.VentaResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ventas")
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    @GetMapping
    public List<VentaResponse> listar() {
        return ventaService.listar();
    }

    @GetMapping("/{id}")
    public VentaResponse obtenerPorId(@PathVariable Long id) {
        return ventaService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VentaResponse crear(@RequestBody VentaRequest request) {
        return ventaService.crear(request);
    }

    @PatchMapping("/{id}/cancelar")
    public VentaResponse cancelar(@PathVariable Long id) {
        return ventaService.cancelar(id);
    }
}