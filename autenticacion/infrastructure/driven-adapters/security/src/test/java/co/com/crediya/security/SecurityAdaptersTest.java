package co.com.crediya.security;

import co.com.crediya.model.rol.Rol;
import co.com.crediya.model.seguridad.Token;
import co.com.crediya.model.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityAdaptersTest {

    private static final String SECRETO = "clave-secreta-de-pruebas-con-32-caracteres-o-mas";

    private static NimbusJwtDecoder decoder(String secreto) {
        return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256).build();
    }

    private static User cliente() {
        return User.builder()
                .id(7L)
                .correoElectronico("cliente@correo.com")
                .documentoIdentidad("1234567890")
                .rol(Rol.builder().id(3L).nombre(Rol.CLIENTE).build())
                .build();
    }

    @Test
    void cifraLaClaveYLaVerifica() {
        BCryptCifradorClaves cifrador = new BCryptCifradorClaves();

        String cifrada = cifrador.cifrar("ClaveSegura1");

        assertNotEquals("ClaveSegura1", cifrada);
        assertTrue(cifrada.startsWith("$2"), "Debe ser un hash BCrypt");
        assertTrue(cifrador.coincide("ClaveSegura1", cifrada));
        assertFalse(cifrador.coincide("OtraClave", cifrada));
        assertNotEquals(cifrada, cifrador.cifrar("ClaveSegura1"), "BCrypt usa una sal distinta cada vez");
    }

    @Test
    void reconoceHashGeneradoPorPostgresConPgcrypto() {
        // Hash de "Admin123*" con el formato $2a$ que produce crypt(..., gen_salt('bf')) en PostgreSQL
        BCryptCifradorClaves cifrador = new BCryptCifradorClaves();
        String hashPostgres = cifrador.cifrar("Admin123*").replaceFirst("^\\$2b\\$", "\\$2a\\$");

        assertTrue(cifrador.coincide("Admin123*", hashPostgres));
    }

    @Test
    void noFallaConValoresNulosOVacios() {
        BCryptCifradorClaves cifrador = new BCryptCifradorClaves();

        assertFalse(cifrador.coincide(null, "$2a$10$x"));
        assertFalse(cifrador.coincide("x", null));
        assertFalse(cifrador.coincide("x", " "));
    }

    @Test
    void generaUnJwtFirmadoConCorreoRolYDocumento() {
        Clock reloj = Clock.fixed(Instant.now(), ZoneOffset.UTC);
        JwtGeneradorToken generador = new JwtGeneradorToken(SECRETO, 3600, reloj);

        Token token = generador.generar(cliente());
        Jwt jwt = decoder(SECRETO).decode(token.valor());

        assertEquals(3600, token.expiraEnSegundos());
        assertEquals("cliente@correo.com", jwt.getSubject());
        assertEquals(Rol.CLIENTE, jwt.getClaimAsString(JwtGeneradorToken.CLAIM_ROL));
        assertEquals("1234567890", jwt.getClaimAsString(JwtGeneradorToken.CLAIM_DOCUMENTO));
        assertEquals(7L, ((Number) jwt.getClaim(JwtGeneradorToken.CLAIM_ID_USUARIO)).longValue());
        assertEquals(JwtGeneradorToken.EMISOR, jwt.getClaimAsString("iss"));
        assertEquals(reloj.instant().plusSeconds(3600).getEpochSecond(), jwt.getExpiresAt().getEpochSecond());
    }

    @Test
    void unTokenFirmadoConOtroSecretoNoEsValido() {
        Token token = new JwtGeneradorToken(SECRETO, 3600, Clock.systemUTC()).generar(cliente());

        assertThrows(JwtException.class,
                () -> decoder("otro-secreto-distinto-de-32-caracteres-minimo").decode(token.valor()));
    }

    @Test
    void exigeUnSecretoDeAlMenos32Caracteres() {
        assertThrows(IllegalStateException.class, () -> new JwtGeneradorToken("corto", 3600, Clock.systemUTC()));
        assertThrows(IllegalStateException.class, () -> new JwtGeneradorToken(null, 3600, Clock.systemUTC()));
    }
}
