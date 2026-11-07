package co.com.crediya.usecase.user;

import co.com.crediya.model.exceptions.CorreoYaRegistradoException;
import co.com.crediya.model.exceptions.DatosInvalidosException;
import co.com.crediya.model.exceptions.DocumentoYaRegistradoException;
import co.com.crediya.model.exceptions.RolNoExisteException;
import co.com.crediya.model.rol.Rol;
import co.com.crediya.model.rol.gateways.RolRepository;
import co.com.crediya.model.seguridad.gateways.CifradorClaves;
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
    static final int CLAVE_LONGITUD_MINIMA = 8;
    static final int DOCUMENTO_LONGITUD_MAXIMA = 20;
    private static final Pattern FORMATO_CORREO =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern FORMATO_DOCUMENTO = Pattern.compile("^[A-Za-z0-9]+$");

    private final UserRepository userRepository;
    private final RolRepository rolRepository;
    private final CifradorClaves cifradorClaves;

    public User registrarUsuario(User user) {
        log.info("Iniciando registro de usuario");

        validarDatos(user);
        User usuarioNormalizado = normalizar(user);
        String correo = usuarioNormalizado.getCorreoElectronico();
        String documento = usuarioNormalizado.getDocumentoIdentidad();

        if (userRepository.existsByCorreoElectronico(correo)) {
            log.warning("Registro rechazado: el correo " + correo + " ya está registrado");
            throw new CorreoYaRegistradoException(correo);
        }
        if (userRepository.existsByDocumentoIdentidad(documento)) {
            log.warning("Registro rechazado: el documento " + documento + " ya está registrado");
            throw new DocumentoYaRegistradoException(documento);
        }

        Long idRol = user.getRol().getId();
        Rol rol = rolRepository.findById(idRol).orElseThrow(() -> {
            log.warning("Registro rechazado: el rol " + idRol + " no existe");
            return new RolNoExisteException(idRol);
        });

        // La clave nunca se guarda ni se registra en logs en texto plano
        User usuarioAGuardar = usuarioNormalizado.toBuilder()
                .clave(cifradorClaves.cifrar(user.getClave()))
                .rol(rol)
                .build();

        User usuarioGuardado = userRepository.save(usuarioAGuardar);
        log.info("Usuario registrado exitosamente con id " + usuarioGuardado.getId() + " y rol " + rol.getNombre());
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

        String documento = user.getDocumentoIdentidad();
        if (estaVacio(documento)) {
            errores.add("El campo documento_identidad es obligatorio");
        } else if (documento.trim().length() > DOCUMENTO_LONGITUD_MAXIMA) {
            errores.add("El campo documento_identidad no puede tener más de " + DOCUMENTO_LONGITUD_MAXIMA + " caracteres");
        } else if (!FORMATO_DOCUMENTO.matcher(documento.trim()).matches()) {
            errores.add("El campo documento_identidad solo puede contener letras y números");
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

        if (estaVacio(user.getClave())) {
            errores.add("El campo clave es obligatorio");
        } else if (user.getClave().length() < CLAVE_LONGITUD_MINIMA) {
            errores.add("El campo clave debe tener al menos " + CLAVE_LONGITUD_MINIMA + " caracteres");
        }

        if (user.getRol() == null || user.getRol().getId() == null) {
            errores.add("El campo id_rol es obligatorio");
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
                .documentoIdentidad(user.getDocumentoIdentidad().trim())
                .correoElectronico(user.getCorreoElectronico().trim().toLowerCase(Locale.ROOT))
                .build();
    }

    private static boolean estaVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
