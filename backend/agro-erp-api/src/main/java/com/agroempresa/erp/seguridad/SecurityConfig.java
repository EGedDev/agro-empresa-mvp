package com.agroempresa.erp.seguridad;

import com.agroempresa.erp.common.tracing.RequestTraceContext;
import com.agroempresa.erp.idempotencia.IdempotencyFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            IdempotencyFilter idempotencyFilter
    ) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/web/**", "/media/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/web/clientes").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/web/solicitudes-atencion").permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/bootstrap-admin").permitAll()
                        .requestMatchers("/api/v1/auth/me").authenticated()
                        .requestMatchers("/api/v1/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/auditoria/**").hasAnyRole("ADMIN", "GERENCIA")
                        .requestMatchers("/api/v1/reportes/**").hasAnyRole("ADMIN", "GERENCIA")
                        .requestMatchers("/api/v1/finanzas/**").hasAnyRole("ADMIN", "GERENCIA")
                        .requestMatchers("/api/v1/comercial/solicitudes-atencion/**")
                        .hasAnyRole("ADMIN", "VENTAS", "GERENCIA")
                        .requestMatchers("/api/v1/clientes/**", "/api/v1/ventas/**")
                        .hasAnyRole("ADMIN", "VENTAS", "GERENCIA")
                        .requestMatchers("/api/v1/proveedores/**", "/api/v1/compras/**")
                        .hasAnyRole("ADMIN", "COMPRAS", "GERENCIA")
                        .requestMatchers("/api/v1/inventario/**")
                        .hasAnyRole("ADMIN", "INVENTARIO", "GERENCIA")
                        .requestMatchers("/api/v1/categorias/**", "/api/v1/productos/**")
                        .hasAnyRole("ADMIN", "INVENTARIO", "VENTAS", "COMPRAS", "GERENCIA")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                )
                .addFilterAfter(idempotencyFilter, BearerTokenAuthenticationFilter.class)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                escribirError(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Autenticacion requerida")
                        )
                        .accessDeniedHandler((request, response, exception) ->
                                escribirError(response, HttpStatus.FORBIDDEN, "FORBIDDEN", "No tienes permisos para esta operacion")
                        )
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::convertirRoles);
        return converter;
    }

    @Bean
    public SecretKey jwtSecretKey(JwtProperties jwtProperties) {
        return new SecretKeySpec(
                jwtProperties.secret().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return NimbusJwtEncoder.withSecretKey(jwtSecretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey, JwtProperties jwtProperties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(jwtProperties.issuer()));
        return decoder;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Idempotency-Key",
                "X-Correlation-Id",
                "X-Request-Id"
        ));
        configuration.setExposedHeaders(List.of(
                "X-Correlation-Id",
                "Idempotency-Replayed"
        ));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private Collection<GrantedAuthority> convertirRoles(Jwt jwt) {
        String rol = jwt.getClaimAsString("rol");

        if (rol == null || rol.isBlank()) {
            return List.of();
        }

        return List.of(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private void escribirError(
            HttpServletResponse response,
            HttpStatus status,
            String codigo,
            String mensaje
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"code":"%s","message":"%s","correlationId":%s,"timestamp":"%s"}\
                """.formatted(codigo, mensaje, jsonStringOrNull(RequestTraceContext.correlationIdActual()), Instant.now()));
    }

    private String jsonStringOrNull(String valor) {
        return valor == null ? "null" : "\"" + valor.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
