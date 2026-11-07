package co.com.crediya.model.exceptions;

/**
 * Se lanza cuando se intenta registrar un documento de identidad que ya pertenece a otro usuario.
 */
public class DocumentoYaRegistradoException extends BusinessException {

    public DocumentoYaRegistradoException(String documentoIdentidad) {
        super("El documento de identidad " + documentoIdentidad + " ya está registrado");
    }
}
