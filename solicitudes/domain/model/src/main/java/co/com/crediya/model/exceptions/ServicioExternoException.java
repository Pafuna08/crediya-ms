package co.com.crediya.model.exceptions;

/**
 * Se lanza cuando no se pudo obtener información de otro sistema del que depende la operación
 * (por ejemplo, el microservicio de autenticación no responde).
 */
public class ServicioExternoException extends RuntimeException {

    public ServicioExternoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
