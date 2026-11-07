package co.com.crediya.model.solicitud;

import co.com.crediya.model.estado.Estado;
import co.com.crediya.model.tipoprestamo.TipoPrestamo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Solicitud de préstamo enviada por un cliente.
 * No depende de ningún framework: ni Spring, ni JPA, ni Jackson.
 */
@Getter
@AllArgsConstructor
@Builder(toBuilder = true)
public class Solicitud {
    private final Long id;
    private final String documentoIdentidad;
    private final BigDecimal monto;
    /** Plazo en meses. */
    private final Integer plazo;
    private final TipoPrestamo tipoPrestamo;
    private final Estado estado;
    private final LocalDateTime fechaCreacion;
}
