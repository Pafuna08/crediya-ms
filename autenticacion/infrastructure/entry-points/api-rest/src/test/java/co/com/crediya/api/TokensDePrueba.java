package co.com.crediya.api;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Firma tokens JWT para las pruebas, igual que lo hace el microservicio al iniciar sesión.
 */
public final class TokensDePrueba {

    public static final String SECRETO = "secreto-de-pruebas-con-mas-de-32-caracteres";
    public static final String PROPIEDAD_SECRETO = "security.jwt.secret=" + SECRETO;
    private static final String EMISOR = "crediya-autenticacion";

    private TokensDePrueba() {
    }

    public static String bearer(String rol) {
        return "Bearer " + token(SECRETO, rol, EMISOR, Instant.now().plusSeconds(3600));
    }

    public static String bearerFirmadoConOtroSecreto(String rol) {
        return "Bearer " + token("otro-secreto-que-no-conoce-el-servidor-123", rol, EMISOR, Instant.now().plusSeconds(3600));
    }

    public static String bearerVencido(String rol) {
        return "Bearer " + token(SECRETO, rol, EMISOR, Instant.now().minusSeconds(3600));
    }

    public static String bearerDeOtroEmisor(String rol) {
        return "Bearer " + token(SECRETO, rol, "otro-sistema", Instant.now().plusSeconds(3600));
    }

    private static String token(String secreto, String rol, String emisor, Instant expira) {
        NimbusJwtEncoder encoder = NimbusJwtEncoder
                .withSecretKey(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .algorithm(MacAlgorithm.HS256)
                .build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(emisor)
                .subject(rol.toLowerCase() + "@crediya.com")
                .issuedAt(expira.minusSeconds(7200))
                .expiresAt(expira)
                .claim("rol", rol)
                .claim("documento", "1000000000")
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
