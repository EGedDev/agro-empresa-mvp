package com.agroempresa.erp.catalogo.producto;

import com.agroempresa.erp.catalogo.producto.dto.ActualizarProductoRequest;
import com.agroempresa.erp.catalogo.producto.dto.ProductoRequest;
import com.agroempresa.erp.catalogo.producto.dto.ProductoResponse;
import com.agroempresa.erp.common.pagination.PaginaResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/productos")
@Validated
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public PaginaResponse<ProductoResponse> listar(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Boolean stockBajo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return productoService.listar(buscar, activo, categoriaId, stockBajo, page, size, sort);
    }

    @GetMapping("/activos")
    public PaginaResponse<ProductoResponse> listarActivos(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return productoService.listarActivos(page, size, sort);
    }

    @GetMapping("/stock-bajo")
    public PaginaResponse<ProductoResponse> listarConStockBajo(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return productoService.listarConStockBajo(page, size, sort);
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
            @Valid @RequestBody ActualizarProductoRequest request
    ) {
        return productoService.actualizar(id, request);
    }

    @PostMapping(path = "/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductoResponse actualizarImagen(
            @PathVariable @Positive(message = "El id debe ser mayor a cero") Long id,
            @RequestPart("imagen") MultipartFile imagen
    ) {
        return productoService.actualizarImagen(id, imagen);
    }

    @PatchMapping("/{id}/desactivar")
    public ProductoResponse desactivar(@PathVariable @Positive(message = "El id debe ser mayor a cero") Long id) {
        return productoService.desactivar(id);
    }
}
