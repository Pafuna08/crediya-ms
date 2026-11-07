package co.com.crediya.model.exceptions;

/**
 * Excepción base para errores de reglas de negocio.
 * Los adaptadores de entrada las traducen a respuestas HTTP controladas.
 */
public abstract class BusinessException extends RuntimeException {

    protected BusinessException(String message) {
        super(message);
    }
}
