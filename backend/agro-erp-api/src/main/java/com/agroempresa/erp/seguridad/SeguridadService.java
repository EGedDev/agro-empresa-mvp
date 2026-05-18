package com.agroempresa.erp.seguridad;

import com.agroempresa.erp.auditoria.AuditoriaService;
import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.common.error.RecursoNoEncontradoException;
import com.agroempresa.erp.seguridad.dto.BootstrapAdminRequest;
import com.agroempresa.erp.seguridad.dto.LoginRequest;
import com.agroempresa.erp.seguridad.dto.LoginResponse;
import com.agroempresa.erp.seguridad.dto.UsuarioResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeguridadService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditoriaService auditoriaService;

    public SeguridadService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditoriaService auditoriaService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UsuarioResponse crearPrimerAdministrador(BootstrapAdminRequest request) {
        validarBootstrapRequest(request);

        if (usuarioRepository.count() > 0) {
            throw new BusinessException("El administrador inicial ya fue creado");
        }

        Usuario usuario = new Usuario(
                normalizarUsername(request.username()),
                passwordEncoder.encode(request.password()),
                request.nombre().trim(),
                RolUsuario.ADMIN
        );

        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        auditoriaService.registrar(
                "ADMIN_INICIAL_CREADO",
                "USUARIO",
                usuarioGuardado.getId(),
                usuarioGuardado.getUsername()
        );

        return UsuarioResponse.desdeEntidad(usuarioGuardado);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        validarLoginRequest(request);

        Usuario usuario = usuarioRepository.findByUsername(normalizarUsername(request.username()))
                .orElseThrow(() -> new BusinessException("Credenciales invalidas"));

        if (!usuario.getActivo()) {
            throw new BusinessException("El usuario se encuentra inactivo");
        }

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new BusinessException("Credenciales invalidas");
        }

        JwtService.TokenGenerado token = jwtService.generarToken(usuario);
        return new LoginResponse(
                token.valor(),
                "Bearer",
                token.expiraEn(),
                UsuarioResponse.desdeEntidad(usuario)
        );
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorUsername(String username) {
        Usuario usuario = usuarioRepository.findByUsername(normalizarUsername(username))
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro el usuario autenticado"));

        return UsuarioResponse.desdeEntidad(usuario);
    }

    private void validarBootstrapRequest(BootstrapAdminRequest request) {
        if (request == null) {
            throw new BusinessException("Los datos del administrador son obligatorios");
        }

        if (request.username() == null || request.username().isBlank()) {
            throw new BusinessException("El usuario administrador es obligatorio");
        }

        if (request.password() == null || request.password().length() < 8) {
            throw new BusinessException("La contrasena debe tener al menos 8 caracteres");
        }

        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new BusinessException("El nombre del usuario es obligatorio");
        }
    }

    private void validarLoginRequest(LoginRequest request) {
        if (request == null) {
            throw new BusinessException("Las credenciales son obligatorias");
        }

        if (request.username() == null || request.username().isBlank()) {
            throw new BusinessException("El usuario es obligatorio");
        }

        if (request.password() == null || request.password().isBlank()) {
            throw new BusinessException("La contrasena es obligatoria");
        }
    }

    private String normalizarUsername(String username) {
        return username.trim().toLowerCase();
    }
}
