package co.com.crediya.model.solicitud;

import lombok.Builder;

/**
 * Filtros opcionales del listado de solicitudes para revisión. Un valor nulo significa "sin filtrar".
 *
 * @param estado             nombre del estado (uno de los estados que requieren revisión)
 * @param idTipoPrestamo     id del tipo de préstamo
 * @param documentoIdentidad documento del cliente
 */
@Builder(toBuilder = true)
public record FiltroSolicitudes(String estado, Long idTipoPrestamo, String documentoIdentidad) {

    public static FiltroSolicitudes sinFiltros() {
        return new FiltroSolicitudes(null, null, null);
    }
}
