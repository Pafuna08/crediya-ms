package co.com.crediya.jpa;

import co.com.crediya.jpa.entity.TipoPrestamoEntity;
import co.com.crediya.jpa.repository.TipoPrestamoJpaRepository;
import co.com.crediya.model.tipoprestamo.TipoPrestamo;
import co.com.crediya.model.tipoprestamo.gateways.TipoPrestamoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Adaptador de salida: consulta los tipos de préstamo existentes.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TipoPrestamoRepositoryAdapter implements TipoPrestamoRepository {

    private final TipoPrestamoJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<TipoPrestamo> findById(Long id) {
        log.debug("Consultando tipo de préstamo con id {}", id);
        if (id == null || id > Integer.MAX_VALUE || id < Integer.MIN_VALUE) {
            return Optional.empty();
        }
        return repository.findById(id.intValue()).map(TipoPrestamoRepositoryAdapter::toDomain);
    }

    private static TipoPrestamo toDomain(TipoPrestamoEntity entity) {
        return TipoPrestamo.builder()
                .id(entity.getIdTipoPrestamo().longValue())
                .nombre(entity.getNombre())
                .tasaInteres(entity.getTasaInteres())
                .build();
    }
}
