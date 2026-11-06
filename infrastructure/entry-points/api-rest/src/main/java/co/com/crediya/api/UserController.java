package co.com.crediya.api;

import co.com.crediya.api.dto.RegistrarUsuarioRequest;
import co.com.crediya.api.dto.UsuarioResponse;
import co.com.crediya.model.user.User;
import co.com.crediya.usecase.user.UserUseCase;
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
 * Adaptador de entrada: recibe las peticiones HTTP y delega en el caso de uso.
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/usuarios", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class UserController {

    private final UserUseCase userUseCase;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UsuarioResponse> registrarUsuario(@RequestBody RegistrarUsuarioRequest request) {
        log.info("POST /api/v1/usuarios - solicitud de registro recibida");

        User registrado = userUseCase.registrarUsuario(request.toDomain());

        log.info("POST /api/v1/usuarios - usuario creado con id {}", registrado.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.fromDomain(registrado));
    }
}
