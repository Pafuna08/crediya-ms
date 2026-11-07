package co.com.crediya.jpa.repository;

import co.com.crediya.jpa.entity.EstadoEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface EstadoJpaRepository extends CrudRepository<EstadoEntity, Integer> {

    Optional<EstadoEntity> findByNombre(String nombre);
}
