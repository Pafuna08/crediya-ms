package co.com.crediya.security;

import co.com.crediya.model.seguridad.gateways.CifradorClaves;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida: cifra y verifica claves con BCrypt.
 * BCrypt agrega una "sal" aleatoria, así que la misma clave produce un cifrado distinto cada vez.
 */
@Component
public class BCryptCifradorClaves implements CifradorClaves {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String cifrar(String claveEnTextoPlano) {
        return encoder.encode(claveEnTextoPlano);
    }

    @Override
    public boolean coincide(String claveEnTextoPlano, String claveCifrada) {
        if (claveEnTextoPlano == null || claveCifrada == null || claveCifrada.isBlank()) {
            return false;
        }
        return encoder.matches(claveEnTextoPlano, claveCifrada);
    }
}
