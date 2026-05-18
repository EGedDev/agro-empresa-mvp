package com.agroempresa.erp.seguridad;

import com.agroempresa.erp.common.pagination.PaginaResponse;
import com.agroempresa.erp.seguridad.dto.ActualizarUsuarioRequest;
import com.agroempresa.erp.seguridad.dto.CrearUsuarioRequest;
import com.agroempresa.erp.seguridad.dto.UsuarioResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public PaginaResponse<UsuarioResponse> listar(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) RolUsuario rol,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return usuarioService.listar(buscar, rol, activo, page, size, sort);
    }

    @GetMapping("/{id}")
    public UsuarioResponse obtenerPorId(@PathVariable Long id) {
        return usuarioService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody CrearUsuarioRequest request) {
        return usuarioService.crear(request);
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarUsuarioRequest request
    ) {
        return usuarioService.actualizar(id, request);
    }

    @PatchMapping("/{id}/desactivar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        usuarioService.desactivar(id);
    }
}
