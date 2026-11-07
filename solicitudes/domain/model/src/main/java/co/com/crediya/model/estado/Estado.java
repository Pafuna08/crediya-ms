package co.com.crediya.model.estado;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Estado en el que se encuentra una solicitud de préstamo.
 */
@Getter
@AllArgsConstructor
@Builder(toBuilder = true)
public class Estado {

    /** Estado con el que se registra toda solicitud nueva (HU2). */
    public static final String PENDIENTE_REVISION = "Pendiente de revisión";

    private final Long id;
    private final String nombre;
    private final String descripcion;
}
