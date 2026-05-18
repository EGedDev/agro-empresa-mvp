package com.agroempresa.erp.catalogo.categoria;

import com.agroempresa.erp.catalogo.categoria.dto.CategoriaRequest;
import com.agroempresa.erp.catalogo.categoria.dto.CategoriaResponse;
import com.agroempresa.erp.common.pagination.PaginaResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/categorias")
@Validated
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public PaginaResponse<CategoriaResponse> listar(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return categoriaService.listar(buscar, activo, page, size, sort);
    }

    @GetMapping("/{id}")
    public CategoriaResponse obtenerPorId(@PathVariable @Positive(message = "El id debe ser mayor a cero") Long id) {
        return categoriaService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaResponse crear(@Valid @RequestBody CategoriaRequest request) {
        return categoriaService.crear(request);
    }

    @PutMapping("/{id}")
    public CategoriaResponse actualizar(
            @PathVariable @Positive(message = "El id debe ser mayor a cero") Long id,
            @Valid @RequestBody CategoriaRequest request
    ) {
        return categoriaService.actualizar(id, request);
    }

    @PatchMapping("/{id}/desactivar")
    public CategoriaResponse desactivar(@PathVariable @Positive(message = "El id debe ser mayor a cero") Long id) {
        return categoriaService.desactivar(id);
    }
}
