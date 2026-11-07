package co.com.crediya.api.dto;

import co.com.crediya.model.cliente.Cliente;
import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.model.solicitud.SolicitudParaRevision;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Una fila del listado de solicitudes para revisión manual (HU4).
 */
public record SolicitudRevisionResponse(
        @JsonProperty("id_solicitud") Long idSolicitud,
        @JsonProperty("documento_identidad") String documentoIdentidad,
        BigDecimal monto,
        Integer plazo,
        String email,
        String nombre,
        @JsonProperty("tipo_prestamo") String tipoPrestamo,
        @JsonProperty("tasa_interes") BigDecimal tasaInteres,
        @JsonProperty("estado_solicitud") String estadoSolicitud,
        @JsonProperty("salario_base") BigDecimal salarioBase,
        @JsonProperty("deuda_total_mensual_solicitudes_aprobadas") BigDecimal deudaTotalMensualSolicitudesAprobadas,
        @JsonProperty("fecha_creacion") LocalDateTime fechaCreacion
) {

    public static SolicitudRevisionResponse fromDomain(SolicitudParaRevision fila) {
        Solicitud s = fila.solicitud();
        Cliente cliente = fila.cliente();
        return new SolicitudRevisionResponse(
                s.getId(),
                s.getDocumentoIdentidad(),
                s.getMonto(),
                s.getPlazo(),
                cliente == null ? null : cliente.getCorreoElectronico(),
                cliente == null ? null : cliente.nombreCompleto(),
                s.getTipoPrestamo().getNombre(),
                s.getTipoPrestamo().getTasaInteres(),
                s.getEstado().getNombre(),
                cliente == null ? null : cliente.getSalarioBase(),
                fila.deudaTotalMensualSolicitudesAprobadas(),
                s.getFechaCreacion());
    }
}
