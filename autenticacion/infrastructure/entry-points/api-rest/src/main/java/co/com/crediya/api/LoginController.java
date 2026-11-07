package co.com.crediya.api;

import co.com.crediya.api.dto.LoginRequest;
import co.com.crediya.api.dto.LoginResponse;
import co.com.crediya.model.seguridad.Token;
import co.com.crediya.usecase.login.LoginUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada: inicio de sesión con correo y clave.
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/login", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class LoginController {

    private final LoginUseCase loginUseCase;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> iniciarSesion(@RequestBody LoginRequest request) {
        log.info("POST /api/v1/login - solicitud de inicio de sesión recibida");

        Token token = loginUseCase.iniciarSesion(request.correoElectronico(), request.clave());

        log.info("POST /api/v1/login - token emitido");
        return ResponseEntity.ok(LoginResponse.fromDomain(token));
    }
}
