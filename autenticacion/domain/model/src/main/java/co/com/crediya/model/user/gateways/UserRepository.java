package co.com.crediya.model.user.gateways;

import co.com.crediya.model.user.User;

import java.util.Optional;

/**
 * Puerto de salida (gateway): define QUÉ necesita el dominio para persistir usuarios,
 * sin decir CÓMO se hace. La implementación vive en la capa de infraestructura.
 */
public interface UserRepository {

    User save(User user);

    boolean existsByCorreoElectronico(String correoElectronico);

    boolean existsByDocumentoIdentidad(String documentoIdentidad);

    /** Devuelve el usuario con su clave cifrada y su rol, para validar el inicio de sesión. */
    Optional<User> findByCorreoElectronico(String correoElectronico);
}
