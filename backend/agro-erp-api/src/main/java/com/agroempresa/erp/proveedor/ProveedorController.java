package com.agroempresa.erp.proveedor;

import com.agroempresa.erp.common.pagination.PaginaResponse;
import com.agroempresa.erp.proveedor.dto.ProveedorRequest;
import com.agroempresa.erp.proveedor.dto.ProveedorResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/proveedores")
@Validated
public class ProveedorController {

    private final ProveedorService proveedorService;

    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @GetMapping
    public PaginaResponse<ProveedorResponse> listar(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return proveedorService.listar(buscar, activo, page, size, sort);
    }

    @GetMapping("/activos")
    public PaginaResponse<ProveedorResponse> listarActivos(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return proveedorService.listarActivos(page, size, sort);
    }

    @GetMapping("/{id}")
    public ProveedorResponse obtenerPorId(@PathVariable @Positive(message = "El id debe ser mayor a cero") Long id) {
        return proveedorService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProveedorResponse crear(@Valid @RequestBody ProveedorRequest request) {
        return proveedorService.crear(request);
    }

    @PutMapping("/{id}")
    public ProveedorResponse actualizar(
            @PathVariable @Positive(message = "El id debe ser mayor a cero") Long id,
            @Valid @RequestBody ProveedorRequest request
    ) {
        return proveedorService.actualizar(id, request);
    }

    @PatchMapping("/{id}/desactivar")
    public ProveedorResponse desactivar(@PathVariable @Positive(message = "El id debe ser mayor a cero") Long id) {
        return proveedorService.desactivar(id);
    }
}
