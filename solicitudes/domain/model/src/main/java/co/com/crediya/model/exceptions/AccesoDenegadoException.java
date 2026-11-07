package co.com.crediya.model.exceptions;

/**
 * Se lanza cuando un usuario intenta realizar una acción sobre datos que no le pertenecen,
 * por ejemplo crear una solicitud de préstamo a nombre de otra persona.
 */
public class AccesoDenegadoException extends BusinessException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
