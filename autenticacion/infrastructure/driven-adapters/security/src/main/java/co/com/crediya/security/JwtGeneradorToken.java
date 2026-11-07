package co.com.crediya.security;

import co.com.crediya.model.seguridad.Token;
import co.com.crediya.model.seguridad.gateways.GeneradorToken;
import co.com.crediya.model.user.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;

/**
 * Adaptador de salida: emite tokens JWT firmados con HMAC-SHA256.
 * Los demás microservicios validan la firma con la misma clave secreta.
 */
@Slf4j
@Component
public class JwtGeneradorToken implements GeneradorToken {

    public static final String CLAIM_ROL = "rol";
    public static final String CLAIM_DOCUMENTO = "documento";
    public static final String CLAIM_ID_USUARIO = "id_usuario";
    public static final String EMISOR = "crediya-autenticacion";
    static final int LONGITUD_MINIMA_SECRETO = 32;

    private final JwtEncoder encoder;
    private final long expiracionSegundos;
    private final Clock clock;

    public JwtGeneradorToken(@Value("${security.jwt.secret}") String secreto,
                             @Value("${security.jwt.expiracion-segundos:3600}") long expiracionSegundos) {
        this(secreto, expiracionSegundos, Clock.systemUTC());
    }

    JwtGeneradorToken(String secreto, long expiracionSegundos, Clock clock) {
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < LONGITUD_MINIMA_SECRETO) {
            throw new IllegalStateException(
                    "security.jwt.secret debe tener al menos " + LONGITUD_MINIMA_SECRETO + " caracteres");
        }
        SecretKey clave = new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = NimbusJwtEncoder.withSecretKey(clave).algorithm(MacAlgorithm.HS256).build();
        this.expiracionSegundos = expiracionSegundos;
        this.clock = clock;
    }

    @Override
    public Token generar(User usuario) {
        Instant ahora = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .subject(usuario.getCorreoElectronico())
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(expiracionSegundos))
                .claim(CLAIM_ROL, usuario.getRol().getNombre())
                .claim(CLAIM_DOCUMENTO, usuario.getDocumentoIdentidad())
                .claim(CLAIM_ID_USUARIO, usuario.getId())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String valor = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        log.debug("Token emitido para {} con rol {}", usuario.getCorreoElectronico(), usuario.getRol().getNombre());
        return new Token(valor, expiracionSegundos);
    }
}
