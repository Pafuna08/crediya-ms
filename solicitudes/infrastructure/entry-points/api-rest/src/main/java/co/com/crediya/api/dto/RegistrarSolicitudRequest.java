package co.com.crediya.api.dto;

import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.model.tipoprestamo.TipoPrestamo;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Cuerpo JSON que recibe el endpoint POST /api/v1/solicitud.
 * No incluye el estado: siempre lo asigna el sistema.
 */
public record RegistrarSolicitudRequest(
        @JsonProperty("documento_identidad") String documentoIdentidad,
        BigDecimal monto,
        Integer plazo,
        @JsonProperty("id_tipo_prestamo") Long idTipoPrestamo
) {

    public Solicitud toDomain() {
        return Solicitud.builder()
                .documentoIdentidad(documentoIdentidad)
                .monto(monto)
                .plazo(plazo)
                .tipoPrestamo(idTipoPrestamo == null ? null : TipoPrestamo.builder().id(idTipoPrestamo).build())
                .build();
    }
}
