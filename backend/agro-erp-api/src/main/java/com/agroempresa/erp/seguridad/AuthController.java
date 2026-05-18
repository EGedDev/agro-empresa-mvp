package com.agroempresa.erp.seguridad;

import com.agroempresa.erp.seguridad.dto.BootstrapAdminRequest;
import com.agroempresa.erp.seguridad.dto.LoginRequest;
import com.agroempresa.erp.seguridad.dto.LoginResponse;
import com.agroempresa.erp.seguridad.dto.UsuarioResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final SeguridadService seguridadService;

    public AuthController(SeguridadService seguridadService) {
        this.seguridadService = seguridadService;
    }

    @PostMapping("/bootstrap-admin")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crearPrimerAdministrador(@Valid @RequestBody BootstrapAdminRequest request) {
        return seguridadService.crearPrimerAdministrador(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return seguridadService.login(request);
    }

    @GetMapping("/me")
    public UsuarioResponse usuarioActual(Authentication authentication) {
        return seguridadService.obtenerPorUsername(authentication.getName());
    }
}
