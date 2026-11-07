package co.com.crediya.jpa.repository;

import co.com.crediya.jpa.entity.SolicitudEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.QueryByExampleExecutor;

public interface SolicitudJpaRepository
        extends CrudRepository<SolicitudEntity, Integer>, QueryByExampleExecutor<SolicitudEntity> {
}
