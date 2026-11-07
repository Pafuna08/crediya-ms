package co.com.crediya.usecase.login;

import co.com.crediya.model.exceptions.CredencialesInvalidasException;
import co.com.crediya.model.exceptions.DatosInvalidosException;
import co.com.crediya.model.seguridad.Token;
import co.com.crediya.model.seguridad.gateways.CifradorClaves;
import co.com.crediya.model.seguridad.gateways.GeneradorToken;
import co.com.crediya.model.user.User;
import co.com.crediya.model.user.gateways.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Caso de uso: iniciar sesión con correo y clave.
 * El número de intentos es ilimitado: no se bloquea al usuario por claves erradas.
 */
@Log
@RequiredArgsConstructor
public class LoginUseCase {

    private final UserRepository userRepository;
    private final CifradorClaves cifradorClaves;
    private final GeneradorToken generadorToken;

    public Token iniciarSesion(String correoElectronico, String clave) {
        validarDatos(correoElectronico, clave);
        String correo = correoElectronico.trim().toLowerCase(Locale.ROOT);
        log.info("Intento de inicio de sesión para " + correo);

        Optional<User> usuario = userRepository.findByCorreoElectronico(correo);
        if (usuario.isEmpty() || !cifradorClaves.coincide(clave, usuario.get().getClave())) {
            // Mismo error para correo inexistente y clave errada: no se revela qué correos existen
            log.warning("Inicio de sesión rechazado para " + correo + ": credenciales incorrectas");
            throw new CredencialesInvalidasException();
        }

        Token token = generadorToken.generar(usuario.get());
        log.info("Inicio de sesión exitoso para " + correo + " con rol " + usuario.get().getRol().getNombre());
        return token;
    }

    private static void validarDatos(String correo, String clave) {
        List<String> errores = new ArrayList<>();
        if (correo == null || correo.isBlank()) {
            errores.add("El campo correo_electronico es obligatorio");
        }
        if (clave == null || clave.isBlank()) {
            errores.add("El campo clave es obligatorio");
        }
        if (!errores.isEmpty()) {
            log.warning("Inicio de sesión rechazado por datos incompletos: " + errores);
            throw new DatosInvalidosException(errores);
        }
    }
}
