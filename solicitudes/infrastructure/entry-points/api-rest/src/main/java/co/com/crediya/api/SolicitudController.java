package co.com.crediya.api;

import co.com.crediya.api.config.SecurityConfig;
import co.com.crediya.api.dto.PaginaResponse;
import co.com.crediya.api.dto.RegistrarSolicitudRequest;
import co.com.crediya.api.dto.SolicitudResponse;
import co.com.crediya.api.dto.SolicitudRevisionResponse;
import co.com.crediya.model.pagina.Pagina;
import co.com.crediya.model.solicitud.FiltroSolicitudes;
import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.model.solicitud.SolicitudParaRevision;
import co.com.crediya.usecase.listarsolicitudes.ListarSolicitudesUseCase;
import co.com.crediya.usecase.solicitud.SolicitudUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada: solicitudes de préstamo por HTTP.
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/solicitud", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class SolicitudController {

    private final SolicitudUseCase solicitudUseCase;
    private final ListarSolicitudesUseCase listarSolicitudesUseCase;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SolicitudResponse> registrarSolicitud(@RequestBody RegistrarSolicitudRequest request,
                                                                @AuthenticationPrincipal Jwt usuario) {
        log.info("POST /api/v1/solicitud - solicitud de préstamo recibida de {}", usuario.getSubject());

        // El documento de quien inició sesión viene firmado dentro del token: no se puede falsificar
        String documentoSolicitante = usuario.getClaimAsString(SecurityConfig.CLAIM_DOCUMENTO);
        Solicitud registrada = solicitudUseCase.registrarSolicitud(request.toDomain(), documentoSolicitante);

        log.info("POST /api/v1/solicitud - solicitud creada con id {}", registrada.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(SolicitudResponse.fromDomain(registrada));
    }

    /**
     * Listado para revisión manual (HU4). Ejemplo:
     * GET /api/v1/solicitud?pagina=0&amp;tamano=10&amp;estado=Rechazada&amp;id_tipo_prestamo=1&amp;documento_identidad=123
     */
    @GetMapping
    public ResponseEntity<PaginaResponse<SolicitudRevisionResponse>> listarParaRevision(
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "tamano", defaultValue = "10") int tamano,
            @RequestParam(name = "estado", required = false) String estado,
            @RequestParam(name = "id_tipo_prestamo", required = false) Long idTipoPrestamo,
            @RequestParam(name = "documento_identidad", required = false) String documentoIdentidad,
            @AuthenticationPrincipal Jwt usuario) {
        log.info("GET /api/v1/solicitud - listado para revisión pedido por {}", usuario.getSubject());

        Pagina<SolicitudParaRevision> resultado = listarSolicitudesUseCase.listar(
                new FiltroSolicitudes(estado, idTipoPrestamo, documentoIdentidad), pagina, tamano);

        log.info("GET /api/v1/solicitud - {} solicitudes devueltas de {}",
                resultado.contenido().size(), resultado.totalElementos());
        return ResponseEntity.ok(PaginaResponse.fromDomain(resultado, SolicitudRevisionResponse::fromDomain));
    }
}
