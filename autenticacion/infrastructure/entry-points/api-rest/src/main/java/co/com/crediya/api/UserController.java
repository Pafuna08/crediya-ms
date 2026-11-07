package co.com.crediya.api;

import co.com.crediya.api.dto.RegistrarUsuarioRequest;
import co.com.crediya.api.dto.UsuarioResponse;
import co.com.crediya.model.user.User;
import co.com.crediya.usecase.consultarusuarios.ConsultarUsuariosUseCase;
import co.com.crediya.usecase.user.UserUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptador de entrada: recibe las peticiones HTTP y delega en el caso de uso.
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/usuarios", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class UserController {

    private final UserUseCase userUseCase;
    private final ConsultarUsuariosUseCase consultarUsuariosUseCase;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UsuarioResponse> registrarUsuario(@RequestBody RegistrarUsuarioRequest request) {
        log.info("POST /api/v1/usuarios - solicitud de registro recibida");

        User registrado = userUseCase.registrarUsuario(request.toDomain());

        log.info("POST /api/v1/usuarios - usuario creado con id {}", registrado.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.fromDomain(registrado));
    }

    /** Ejemplo: GET /api/v1/usuarios?documentos=3000000000,4000000000 */
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> buscarPorDocumentos(@RequestParam("documentos") List<String> documentos) {
        log.info("GET /api/v1/usuarios - consulta de {} documentos", documentos.size());

        List<UsuarioResponse> usuarios = consultarUsuariosUseCase.buscarPorDocumentos(documentos).stream()
                .map(UsuarioResponse::fromDomain)
                .toList();

        log.info("GET /api/v1/usuarios - {} usuarios encontrados", usuarios.size());
        return ResponseEntity.ok(usuarios);
    }
}
