package com.agroempresa.erp.seguridad;

import com.agroempresa.erp.auditoria.AuditoriaService;
import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.common.error.RecursoNoEncontradoException;
import com.agroempresa.erp.common.pagination.PaginaResponse;
import com.agroempresa.erp.common.pagination.Paginacion;
import com.agroempresa.erp.seguridad.dto.ActualizarUsuarioRequest;
import com.agroempresa.erp.seguridad.dto.CrearUsuarioRequest;
import com.agroempresa.erp.seguridad.dto.UsuarioResponse;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class UsuarioService {

    private static final Map<String, String> CAMPOS_ORDENABLES = Map.of(
            "id", "id",
            "username", "username",
            "nombre", "nombre",
            "rol", "rol",
            "activo", "activo",
            "creadoEn", "creadoEn",
            "actualizadoEn", "actualizadoEn"
    );

    private static final Sort ORDEN_DEFAULT = Sort.by(Sort.Direction.ASC, "username");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            AuditoriaService auditoriaService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<UsuarioResponse> listar(
            String buscar,
            RolUsuario rol,
            Boolean activo,
            Integer pagina,
            Integer tamanio,
            String orden
    ) {
        return PaginaResponse.desde(
                usuarioRepository.buscar(
                        Paginacion.normalizarTexto(buscar),
                        rol,
                        activo,
                        Paginacion.crear(pagina, tamanio, orden, CAMPOS_ORDENABLES, ORDEN_DEFAULT)
                ),
                UsuarioResponse::desdeEntidad
        );
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorId(Long id) {
        return UsuarioResponse.desdeEntidad(buscarUsuario(id));
    }

    @Transactional
    public UsuarioResponse crear(CrearUsuarioRequest request) {
        validarCrearUsuario(request);

        String username = normalizarUsername(request.username());

        if (usuarioRepository.existsByUsername(username)) {
            throw new BusinessException("Ya existe un usuario con ese nombre");
        }

        Usuario usuario = new Usuario(
                username,
                passwordEncoder.encode(request.password()),
                request.nombre().trim(),
                request.rol()
        );

        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        auditoriaService.registrar("USUARIO_CREADO", "USUARIO", usuarioGuardado.getId(), username);

        return UsuarioResponse.desdeEntidad(usuarioGuardado);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UsuarioResponse actualizar(Long id, ActualizarUsuarioRequest request) {
        if (request == null) {
            throw new BusinessException("Los datos del usuario son obligatorios");
        }

        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new BusinessException("El nombre del usuario es obligatorio");
        }

        if (request.rol() == null) {
            throw new BusinessException("El rol es obligatorio");
        }

        Usuario usuario = buscarUsuario(id);
        validarQueNoSeaUltimoAdminActivo(usuario, request.rol());

        usuario.actualizar(request.nombre().trim(), request.rol());
        auditoriaService.registrar("USUARIO_ACTUALIZADO", "USUARIO", usuario.getId(), usuario.getUsername());

        return UsuarioResponse.desdeEntidad(usuario);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void desactivar(Long id) {
        Usuario usuario = buscarUsuario(id);

        if (!usuario.getActivo()) {
            return;
        }

        validarQueNoSeaUltimoAdminActivo(usuario, null);

        usuario.desactivar();
        auditoriaService.registrar("USUARIO_DESACTIVADO", "USUARIO", usuario.getId(), usuario.getUsername());
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro el usuario con id: " + id));
    }

    private void validarCrearUsuario(CrearUsuarioRequest request) {
        if (request == null) {
            throw new BusinessException("Los datos del usuario son obligatorios");
        }

        if (request.username() == null || request.username().isBlank()) {
            throw new BusinessException("El usuario es obligatorio");
        }

        if (request.password() == null || request.password().length() < 8) {
            throw new BusinessException("La contrasena debe tener al menos 8 caracteres");
        }

        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new BusinessException("El nombre del usuario es obligatorio");
        }

        if (request.rol() == null) {
            throw new BusinessException("El rol es obligatorio");
        }
    }

    private void validarQueNoSeaUltimoAdminActivo(Usuario usuario, RolUsuario nuevoRol) {
        if (usuario.getRol() != RolUsuario.ADMIN || !usuario.getActivo()) {
            return;
        }

        if (nuevoRol == RolUsuario.ADMIN) {
            return;
        }

        if (usuarioRepository.countByRolAndActivoTrue(RolUsuario.ADMIN) <= 1) {
            throw new BusinessException("No se puede dejar el sistema sin administradores activos");
        }
    }

    private String normalizarUsername(String username) {
        return username.trim().toLowerCase();
    }
}
