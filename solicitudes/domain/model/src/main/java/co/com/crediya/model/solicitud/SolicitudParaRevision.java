package co.com.crediya.model.solicitud;

import co.com.crediya.model.cliente.Cliente;

import java.math.BigDecimal;

/**
 * Una fila del listado que revisa el asesor (HU4): la solicitud, los datos del cliente
 * y cuánto paga ya ese cliente al mes por sus préstamos aprobados.
 *
 * @param solicitud                           la solicitud a revisar
 * @param cliente                             datos del cliente; puede ser nulo si ya no existe en autenticación
 * @param deudaTotalMensualSolicitudesAprobadas suma de las cuotas mensuales de sus préstamos aprobados
 */
public record SolicitudParaRevision(Solicitud solicitud, Cliente cliente,
                                    BigDecimal deudaTotalMensualSolicitudesAprobadas) {
}
