package co.com.crediya.model.user.gateways;

import co.com.crediya.model.user.User;

/**
 * Puerto de salida (gateway): define QUÉ necesita el dominio para persistir usuarios,
 * sin decir CÓMO se hace. La implementación vive en la capa de infraestructura.
 */
public interface UserRepository {

    User save(User user);

    boolean existsByCorreoElectronico(String correoElectronico);
}
