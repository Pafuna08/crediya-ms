package co.com.crediya.model.rol.gateways;

import co.com.crediya.model.rol.Rol;

import java.util.Optional;

public interface RolRepository {

    Optional<Rol> findById(Long id);
}
