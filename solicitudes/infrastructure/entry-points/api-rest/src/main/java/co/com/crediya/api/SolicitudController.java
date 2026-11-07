package co.com.crediya.api;

import co.com.crediya.api.dto.RegistrarSolicitudRequest;
import co.com.crediya.api.dto.SolicitudResponse;
import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.usecase.solicitud.SolicitudUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada: recibe las solicitudes de préstamo por HTTP y delega en el caso de uso.
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/solicitud", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class SolicitudController {

    private final SolicitudUseCase solicitudUseCase;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SolicitudResponse> registrarSolicitud(@RequestBody RegistrarSolicitudRequest request) {
        log.info("POST /api/v1/solicitud - solicitud de préstamo recibida");

        Solicitud registrada = solicitudUseCase.registrarSolicitud(request.toDomain());

        log.info("POST /api/v1/solicitud - solicitud creada con id {}", registrada.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(SolicitudResponse.fromDomain(registrada));
    }
}
