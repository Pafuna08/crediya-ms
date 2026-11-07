package co.com.crediya.jpa;

import co.com.crediya.jpa.entity.UserEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.QueryByExampleExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JPARepository extends CrudRepository<UserEntity, Integer>, QueryByExampleExecutor<UserEntity> {

    boolean existsByEmail(String email);

    boolean existsByDocumentoIdentidad(String documentoIdentidad);

    Optional<UserEntity> findByEmail(String email);

    List<UserEntity> findByDocumentoIdentidadIn(Collection<String> documentos);
}
