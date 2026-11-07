package co.com.crediya.model.exceptions;

/**
 * Se lanza cuando el correo o la clave del inicio de sesión no son correctos.
 * El mensaje es el mismo en ambos casos para no revelar qué correos existen.
 */
public class CredencialesInvalidasException extends BusinessException {

    public CredencialesInvalidasException() {
        super("Correo o clave incorrectos");
    }
}
