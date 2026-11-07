package co.com.crediya.model.seguridad;

/**
 * Credencial que recibe el usuario al iniciar sesión.
 *
 * @param valor            el token firmado
 * @param expiraEnSegundos tiempo de validez desde que se emitió
 */
public record Token(String valor, long expiraEnSegundos) {
}
