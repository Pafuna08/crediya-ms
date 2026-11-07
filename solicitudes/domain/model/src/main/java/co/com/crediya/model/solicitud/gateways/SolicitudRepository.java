package co.com.crediya.model.solicitud.gateways;

import co.com.crediya.model.pagina.Pagina;
import co.com.crediya.model.solicitud.FiltroSolicitudes;
import co.com.crediya.model.solicitud.Solicitud;

import java.util.List;

/**
 * Puerto de salida: lo que el dominio necesita para guardar y consultar solicitudes.
 */
public interface SolicitudRepository {

    Solicitud save(Solicitud solicitud);

    /**
     * Solicitudes en alguno de los estados indicados que cumplen el filtro, de la más antigua a la más reciente.
     * Cada solicitud trae su tipo de préstamo y su estado completos.
     */
    Pagina<Solicitud> findByEstados(List<String> estados, FiltroSolicitudes filtro, int pagina, int tamano);

    /** Todas las solicitudes de esos clientes que están en el estado indicado, con su tipo de préstamo completo. */
    List<Solicitud> findByDocumentosAndEstado(List<String> documentos, String estado);
}
