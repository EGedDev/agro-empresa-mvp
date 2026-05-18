package com.agroempresa.erp.seguridad;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public JwtService(JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
    }

    public TokenGenerado generarToken(Usuario usuario) {
        Instant emitidoEn = Instant.now();
        Instant expiraEn = emitidoEn.plus(jwtProperties.expiration());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(emitidoEn)
                .expiresAt(expiraEn)
                .subject(usuario.getUsername())
                .claim("usuarioId", usuario.getId())
                .claim("rol", usuario.getRol().name())
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return new TokenGenerado(token, expiraEn);
    }

    public record TokenGenerado(
            String valor,
            Instant expiraEn
    ) {
    }
}
