package com.agroempresa.erp.comercial.venta;

import com.agroempresa.erp.comercial.venta.dto.VentaRequest;
import com.agroempresa.erp.comercial.venta.dto.VentaResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ventas")
@Validated
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
