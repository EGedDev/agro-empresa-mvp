package com.agroempresa.erp.seguridad;

import com.agroempresa.erp.auditoria.AuditoriaService;
import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.seguridad.dto.BootstrapAdminRequest;
import com.agroempresa.erp.seguridad.dto.LoginRequest;
import com.agroempresa.erp.seguridad.dto.LoginResponse;
import com.agroempresa.erp.seguridad.dto.UsuarioResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeguridadServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private SeguridadService seguridadService;

    @Test
    void crearPrimerAdministradorNormalizaUsuarioYHasheaPassword() {
        BootstrapAdminRequest request = new BootstrapAdminRequest(
                " Admin ",
                "password-seguro",
                "Administrador"
        );

        when(usuarioRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("password-seguro")).thenReturn("hash-bcrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            ReflectionTestUtils.setField(usuario, "id", 1L);
            return usuario;
        });

        UsuarioResponse response = seguridadService.crearPrimerAdministrador(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("admin");
        assertThat(response.rol()).isEqualTo(RolUsuario.ADMIN);
        verify(passwordEncoder).encode("password-seguro");
        verify(auditoriaService).registrar("ADMIN_INICIAL_CREADO", "USUARIO", 1L, "admin");
    }

    @Test
    void crearPrimerAdministradorRechazaSiYaExistenUsuarios() {
        BootstrapAdminRequest request = new BootstrapAdminRequest(
                "admin",
                "password-seguro",
                "Administrador"
        );

        when(usuarioRepository.count()).thenReturn(1L);

        assertThatThrownBy(() -> seguridadService.crearPrimerAdministrador(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("El administrador inicial ya fue creado");

        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void loginGeneraTokenConCredencialesValidas() {
        Usuario usuario = new Usuario("admin", "hash-bcrypt", "Administrador", RolUsuario.ADMIN);
        ReflectionTestUtils.setField(usuario, "id", 1L);
        Instant expiraEn = Instant.now().plusSeconds(3600);

        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password-seguro", "hash-bcrypt")).thenReturn(true);
        when(jwtService.generarToken(usuario)).thenReturn(new JwtService.TokenGenerado("jwt-token", expiraEn));

        LoginResponse response = seguridadService.login(new LoginRequest(" Admin ", "password-seguro"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresAt()).isEqualTo(expiraEn);
        assertThat(response.usuario().username()).isEqualTo("admin");
    }

    @Test
    void loginRechazaPasswordInvalido() {
        Usuario usuario = new Usuario("admin", "hash-bcrypt", "Administrador", RolUsuario.ADMIN);

        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("incorrecta", "hash-bcrypt")).thenReturn(false);

        assertThatThrownBy(() -> seguridadService.login(new LoginRequest("admin", "incorrecta")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Credenciales invalidas");

        verifyNoInteractions(jwtService);
    }
}
