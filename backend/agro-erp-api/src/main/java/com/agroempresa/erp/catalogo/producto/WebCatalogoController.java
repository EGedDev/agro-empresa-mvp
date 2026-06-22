package com.agroempresa.erp.catalogo.producto;

import com.agroempresa.erp.catalogo.producto.dto.ProductoResponse;
import com.agroempresa.erp.common.pagination.PaginaResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/web/productos")
@Validated
public class WebCatalogoController {

    private final ProductoService productoService;

    public WebCatalogoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public PaginaResponse<ProductoResponse> listarProductosWeb(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return productoService.listarWeb(buscar, categoriaId, page, size, sort);
    }

    @GetMapping("/{id}")
    public ProductoResponse obtenerProductoWeb(@PathVariable Long id) {
        return productoService.obtenerWebPorId(id);
    }
}
