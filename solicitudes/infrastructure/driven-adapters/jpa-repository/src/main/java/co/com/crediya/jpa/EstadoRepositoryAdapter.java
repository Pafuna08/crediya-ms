package co.com.crediya.jpa;

import co.com.crediya.jpa.entity.EstadoEntity;
import co.com.crediya.jpa.repository.EstadoJpaRepository;
import co.com.crediya.model.estado.Estado;
import co.com.crediya.model.estado.gateways.EstadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Adaptador de salida: consulta los estados de solicitud configurados.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class EstadoRepositoryAdapter implements EstadoRepository {

    private final EstadoJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Estado> findByNombre(String nombre) {
        log.debug("Consultando estado '{}'", nombre);
        return repository.findByNombre(nombre).map(EstadoRepositoryAdapter::toDomain);
    }

    private static Estado toDomain(EstadoEntity entity) {
        return Estado.builder()
                .id(entity.getIdEstado().longValue())
                .nombre(entity.getNombre())
                .descripcion(entity.getDescripcion())
                .build();
    }
}
