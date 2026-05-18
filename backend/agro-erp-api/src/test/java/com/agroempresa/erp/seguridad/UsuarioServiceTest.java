package com.agroempresa.erp.seguridad;

import com.agroempresa.erp.auditoria.AuditoriaService;
import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.seguridad.dto.ActualizarUsuarioRequest;
import com.agroempresa.erp.seguridad.dto.CrearUsuarioRequest;
import com.agroempresa.erp.seguridad.dto.UsuarioResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void crearUsuarioNormalizaUsernameHasheaPasswordYRegistraAuditoria() {
        CrearUsuarioRequest request = new CrearUsuarioRequest(
                " Vendedor ",
                "password-seguro",
                "Vendedor Uno",
                RolUsuario.VENTAS
        );

        when(usuarioRepository.existsByUsername("vendedor")).thenReturn(false);
        when(passwordEncoder.encode("password-seguro")).thenReturn("hash-bcrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            ReflectionTestUtils.setField(usuario, "id", 8L);
            return usuario;
        });

        UsuarioResponse response = usuarioService.crear(request);

        assertThat(response.id()).isEqualTo(8L);
        assertThat(response.username()).isEqualTo("vendedor");
        assertThat(response.rol()).isEqualTo(RolUsuario.VENTAS);
        verify(passwordEncoder).encode("password-seguro");
        verify(auditoriaService).registrar("USUARIO_CREADO", "USUARIO", 8L, "vendedor");
    }

    @Test
    void crearUsuarioRechazaUsernameDuplicado() {
        CrearUsuarioRequest request = new CrearUsuarioRequest(
                "vendedor",
                "password-seguro",
                "Vendedor Uno",
                RolUsuario.VENTAS
        );

        when(usuarioRepository.existsByUsername("vendedor")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Ya existe un usuario con ese nombre");

        verifyNoInteractions(passwordEncoder, auditoriaService);
    }

    @Test
    void actualizarRechazaDegradarUltimoAdministradorActivo() {
        Usuario admin = new Usuario("admin", "hash-bcrypt", "Administrador", RolUsuario.ADMIN);
        ReflectionTestUtils.setField(admin, "id", 1L);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.countByRolAndActivoTrue(RolUsuario.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> usuarioService.actualizar(
                1L,
                new ActualizarUsuarioRequest("Administrador", RolUsuario.VENTAS)
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("No se puede dejar el sistema sin administradores activos");

        verifyNoInteractions(auditoriaService);
    }
}
