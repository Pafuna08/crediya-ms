package co.com.crediya.usecase.listarsolicitudes;

import co.com.crediya.model.cliente.Cliente;
import co.com.crediya.model.cliente.gateways.ClienteGateway;
import co.com.crediya.model.estado.Estado;
import co.com.crediya.model.exceptions.DatosInvalidosException;
import co.com.crediya.model.pagina.Pagina;
import co.com.crediya.model.solicitud.FiltroSolicitudes;
import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.model.solicitud.SolicitudParaRevision;
import co.com.crediya.model.solicitud.gateways.SolicitudRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Caso de uso (HU4): listado paginado y filtrable de las solicitudes que necesitan la decisión de un asesor
 * ("Pendiente de revisión", "Rechazada" y "Revision manual"), con los datos del cliente y su deuda mensual
 * por préstamos ya aprobados.
 */
@Log
@RequiredArgsConstructor
public class ListarSolicitudesUseCase {

    static final int TAMANO_MAXIMO = 50;

    private final SolicitudRepository solicitudRepository;
    private final ClienteGateway clienteGateway;

    public Pagina<SolicitudParaRevision> listar(FiltroSolicitudes filtro, int pagina, int tamano) {
        FiltroSolicitudes filtroLimpio = validarYNormalizar(filtro == null ? FiltroSolicitudes.sinFiltros() : filtro,
                pagina, tamano);
        List<String> estados = filtroLimpio.estado() == null
                ? Estado.ESTADOS_PARA_REVISION
                : List.of(filtroLimpio.estado());
        log.info("Listando solicitudes para revisión: estados=" + estados + ", tipo=" + filtroLimpio.idTipoPrestamo()
                + ", documento=" + filtroLimpio.documentoIdentidad() + ", página=" + pagina + ", tamaño=" + tamano);

        Pagina<Solicitud> solicitudes = solicitudRepository.findByEstados(estados, filtroLimpio, pagina, tamano);
        if (solicitudes.contenido().isEmpty()) {
            log.info("No hay solicitudes para revisión con esos filtros");
            return solicitudes.map(s -> new SolicitudParaRevision(s, null, BigDecimal.ZERO));
        }

        List<String> documentos = solicitudes.contenido().stream()
                .map(Solicitud::getDocumentoIdentidad)
                .distinct()
                .toList();

        Map<String, Cliente> clientes = clienteGateway.findByDocumentos(documentos).stream()
                .collect(Collectors.toMap(Cliente::getDocumentoIdentidad, Function.identity(), (a, b) -> a));
        if (clientes.size() < documentos.size()) {
            log.warning("No se encontraron en autenticación " + (documentos.size() - clientes.size())
                    + " de " + documentos.size() + " clientes del listado");
        }

        Map<String, BigDecimal> deudaPorCliente = solicitudRepository
                .findByDocumentosAndEstado(documentos, Estado.APROBADA).stream()
                .collect(Collectors.groupingBy(Solicitud::getDocumentoIdentidad,
                        Collectors.reducing(BigDecimal.ZERO, Solicitud::cuotaMensual, BigDecimal::add)));

        Pagina<SolicitudParaRevision> resultado = solicitudes.map(s -> new SolicitudParaRevision(
                s,
                clientes.get(s.getDocumentoIdentidad()),
                deudaPorCliente.getOrDefault(s.getDocumentoIdentidad(), BigDecimal.ZERO)));
        log.info("Listado generado: " + resultado.contenido().size() + " de " + resultado.totalElementos()
                + " solicitudes (página " + pagina + " de " + resultado.totalPaginas() + ")");
        return resultado;
    }

    private static FiltroSolicitudes validarYNormalizar(FiltroSolicitudes filtro, int pagina, int tamano) {
        List<String> errores = new ArrayList<>();
        if (pagina < 0) {
            errores.add("El parámetro pagina debe ser mayor o igual a 0");
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO) {
            errores.add("El parámetro tamano debe estar entre 1 y " + TAMANO_MAXIMO);
        }

        String estado = null;
        if (filtro.estado() != null && !filtro.estado().isBlank()) {
            String buscado = filtro.estado().trim().toLowerCase(Locale.ROOT);
            estado = Estado.ESTADOS_PARA_REVISION.stream()
                    .filter(e -> e.toLowerCase(Locale.ROOT).equals(buscado))
                    .findFirst()
                    .orElse(null);
            if (estado == null) {
                errores.add("El parámetro estado debe ser uno de: " + String.join(", ", Estado.ESTADOS_PARA_REVISION));
            }
        }

        if (!errores.isEmpty()) {
            log.warning("Listado rechazado por parámetros inválidos: " + errores);
            throw new DatosInvalidosException(errores);
        }

        String documento = filtro.documentoIdentidad() == null || filtro.documentoIdentidad().isBlank()
                ? null : filtro.documentoIdentidad().trim();
        return new FiltroSolicitudes(estado, filtro.idTipoPrestamo(), documento);
    }
}
