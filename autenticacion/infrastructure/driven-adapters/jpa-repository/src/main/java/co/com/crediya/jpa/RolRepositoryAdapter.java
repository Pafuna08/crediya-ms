package co.com.crediya.jpa;

import co.com.crediya.jpa.entity.RolEntity;
import co.com.crediya.model.rol.Rol;
import co.com.crediya.model.rol.gateways.RolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Adaptador de salida: consulta los roles existentes.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RolRepositoryAdapter implements RolRepository {

    private final RolJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Rol> findById(Long id) {
        log.debug("Consultando rol con id {}", id);
        if (id == null || id > Integer.MAX_VALUE || id < Integer.MIN_VALUE) {
            return Optional.empty();
        }
        return repository.findById(id.intValue()).map(RolRepositoryAdapter::toDomain);
    }

    static Rol toDomain(RolEntity entity) {
        return Rol.builder()
                .id(entity.getIdRol().longValue())
                .nombre(entity.getNombre())
                .descripcion(entity.getDescripcion())
                .build();
    }
}
