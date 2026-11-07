package co.com.crediya.api.dto;

import co.com.crediya.model.solicitud.Solicitud;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Respuesta JSON con la solicitud registrada.
 */
public record SolicitudResponse(
        @JsonProperty("id_solicitud") Long idSolicitud,
        @JsonProperty("documento_identidad") String documentoIdentidad,
        BigDecimal monto,
        Integer plazo,
        @JsonProperty("tipo_prestamo") String tipoPrestamo,
        @JsonProperty("tasa_interes") BigDecimal tasaInteres,
        String estado,
        @JsonProperty("fecha_creacion") LocalDateTime fechaCreacion
) {

    public static SolicitudResponse fromDomain(Solicitud solicitud) {
        return new SolicitudResponse(
                solicitud.getId(),
                solicitud.getDocumentoIdentidad(),
                solicitud.getMonto(),
                solicitud.getPlazo(),
                solicitud.getTipoPrestamo().getNombre(),
                solicitud.getTipoPrestamo().getTasaInteres(),
                solicitud.getEstado().getNombre(),
                solicitud.getFechaCreacion());
    }
}
