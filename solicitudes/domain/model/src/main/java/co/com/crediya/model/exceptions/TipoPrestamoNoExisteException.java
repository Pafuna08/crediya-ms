package co.com.crediya.model.exceptions;

/**
 * Se lanza cuando el tipo de préstamo seleccionado no es uno de los existentes.
 */
public class TipoPrestamoNoExisteException extends BusinessException {

    public TipoPrestamoNoExisteException(Long idTipoPrestamo) {
        super("El tipo de préstamo con id " + idTipoPrestamo + " no existe");
    }
}
