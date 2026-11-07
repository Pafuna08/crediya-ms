package co.com.crediya.model.exceptions;

/**
 * Se lanza cuando el rol indicado para un usuario no es uno de los existentes.
 */
public class RolNoExisteException extends BusinessException {

    public RolNoExisteException(Long idRol) {
        super("El rol con id " + idRol + " no existe");
    }
}
