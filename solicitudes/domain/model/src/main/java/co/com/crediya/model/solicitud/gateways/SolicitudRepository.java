package co.com.crediya.model.solicitud.gateways;

import co.com.crediya.model.solicitud.Solicitud;

/**
 * Puerto de salida: lo que el dominio necesita para guardar solicitudes.
 */
public interface SolicitudRepository {

    Solicitud save(Solicitud solicitud);
}
