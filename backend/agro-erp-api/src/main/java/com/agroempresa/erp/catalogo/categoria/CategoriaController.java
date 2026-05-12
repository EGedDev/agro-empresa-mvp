package com.agroempresa.erp.catalogo.categoria;

import com.agroempresa.erp.catalogo.categoria.dto.CategoriaRequest;
import com.agroempresa.erp.catalogo.categoria.dto.CategoriaResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public List<CategoriaResponse> listar() {
        return categoriaService.listar();
    }

    @GetMapping("/{id}")
    public CategoriaResponse obtenerPorId(@PathVariable Long id) {
        return categoriaService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaResponse crear(@Valid @RequestBody CategoriaRequest request) {
        return categoriaService.crear(request);
    }

    @PutMapping("/{id}")
    public CategoriaResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaRequest request
    ) {
        return categoriaService.actualizar(id, request);
    }

    @PatchMapping("/{id}/desactivar")
    public CategoriaResponse desactivar(@PathVariable Long id) {
        return categoriaService.desactivar(id);
    }
}