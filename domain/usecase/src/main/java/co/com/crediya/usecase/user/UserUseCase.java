package co.com.crediya.usecase.user;

import co.com.crediya.model.exceptions.CorreoYaRegistradoException;
import co.com.crediya.model.exceptions.DatosInvalidosException;
import co.com.crediya.model.user.User;
import co.com.crediya.model.user.gateways.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Caso de uso: registrar un usuario en el sistema.
 * Aquí viven las reglas de negocio; no sabe nada de HTTP ni de base de datos.
 */
@Log
@RequiredArgsConstructor
public class UserUseCase {

    static final BigDecimal SALARIO_MINIMO = BigDecimal.ZERO;
    static final BigDecimal SALARIO_MAXIMO = new BigDecimal("15000000");
    private static final Pattern FORMATO_CORREO =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserRepository userRepository;

    public User registrarUsuario(User user) {
        log.info("Iniciando registro de usuario");

        validarDatos(user);
        User usuarioNormalizado = normalizar(user);
        String correo = usuarioNormalizado.getCorreoElectronico();

        if (userRepository.existsByCorreoElectronico(correo)) {
            log.warning("Registro rechazado: el correo " + correo + " ya está registrado");
            throw new CorreoYaRegistradoException(correo);
        }

        User usuarioGuardado = userRepository.save(usuarioNormalizado);
        log.info("Usuario registrado exitosamente con id " + usuarioGuardado.getId());
        return usuarioGuardado;
    }

    private void validarDatos(User user) {
        if (user == null) {
            throw new DatosInvalidosException(List.of("Los datos del usuario son obligatorios"));
        }

        List<String> errores = new ArrayList<>();

        if (estaVacio(user.getNombres())) {
            errores.add("El campo nombres es obligatorio");
        }
        if (estaVacio(user.getApellidos())) {
            errores.add("El campo apellidos es obligatorio");
        }

        if (estaVacio(user.getCorreoElectronico())) {
            errores.add("El campo correo_electronico es obligatorio");
        } else if (!FORMATO_CORREO.matcher(user.getCorreoElectronico().trim()).matches()) {
            errores.add("El campo correo_electronico no tiene un formato de email válido");
        }

        BigDecimal salario = user.getSalarioBase();
        if (salario == null) {
            errores.add("El campo salario_base es obligatorio");
        } else if (salario.compareTo(SALARIO_MINIMO) < 0 || salario.compareTo(SALARIO_MAXIMO) > 0) {
            errores.add("El campo salario_base debe estar entre 0 y 15000000");
        }

        if (!errores.isEmpty()) {
            log.warning("Registro rechazado por datos inválidos: " + errores);
            throw new DatosInvalidosException(errores);
        }
    }

    private User normalizar(User user) {
        return user.toBuilder()
                .id(null)
                .nombres(user.getNombres().trim())
                .apellidos(user.getApellidos().trim())
                .correoElectronico(user.getCorreoElectronico().trim().toLowerCase(Locale.ROOT))
                .build();
    }

    private static boolean estaVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
