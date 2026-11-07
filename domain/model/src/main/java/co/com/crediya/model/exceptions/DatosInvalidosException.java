package co.com.crediya.model.exceptions;

import java.util.List;

/**
 * Se lanza cuando los datos de entrada no cumplen las validaciones de negocio.
 * Contiene todos los errores encontrados para informarlos juntos.
 */
public class DatosInvalidosException extends BusinessException {

    private final List<String> errores;

    public DatosInvalidosException(List<String> errores) {
        super("Los datos del usuario no son válidos");
        this.errores = List.copyOf(errores);
    }

    public List<String> getErrores() {
        return errores;
    }
}
