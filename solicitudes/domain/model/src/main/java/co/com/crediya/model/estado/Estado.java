package co.com.crediya.model.estado;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Estado en el que se encuentra una solicitud de préstamo.
 */
@Getter
@AllArgsConstructor
@Builder(toBuilder = true)
public class Estado {

    /** Estado con el que se registra toda solicitud nueva (HU2). */
    public static final String PENDIENTE_REVISION = "Pendiente de revisión";
    public static final String REVISION_MANUAL = "Revision manual";
    public static final String RECHAZADA = "Rechazada";
    public static final String APROBADA = "Aprobada";

    /** Estados que necesitan la decisión de un asesor (HU4). */
    public static final List<String> ESTADOS_PARA_REVISION = List.of(PENDIENTE_REVISION, RECHAZADA, REVISION_MANUAL);

    private final Long id;
    private final String nombre;
    private final String descripcion;
}
