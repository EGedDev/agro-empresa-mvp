package com.agroempresa.erp.catalogo.producto;

import com.agroempresa.erp.catalogo.producto.dto.ProductoRequest;
import com.agroempresa.erp.catalogo.producto.dto.ProductoResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/productos")
@Validated
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public List<ProductoResponse> listar() {
        return productoService.listar();
    }

    @GetMapping("/activos")
    public List<ProductoResponse> listarActivos() {
        return productoService.listarActivos();
    }

    @GetMapping("/stock-bajo")
    public List<ProductoResponse> listarConStockBajo() {
        return productoService.listarConStockBajo();
    }

    @GetMapping("/{id}")
    public ProductoResponse obtenerPorId(@PathVariable @Positive(message = "El id debe ser mayor a cero") Long id) {
        return productoService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest request) {
        return productoService.crear(request);
    }

    @PutMapping("/{id}")
    public ProductoResponse actualizar(
            @PathVariable @Positive(message = "El id debe ser mayor a cero") Long id,
            @Valid @RequestBody ProductoRequest request
    ) {
        return productoService.actualizar(id, request);
    }

    @PatchMapping("/{id}/desactivar")
    public ProductoResponse desactivar(@PathVariable @Positive(message = "El id debe ser mayor a cero") Long id) {
        return productoService.desactivar(id);
    }
}
