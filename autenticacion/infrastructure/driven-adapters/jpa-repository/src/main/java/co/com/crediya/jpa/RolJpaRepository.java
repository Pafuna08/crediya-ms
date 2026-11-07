package co.com.crediya.jpa;

import co.com.crediya.jpa.entity.RolEntity;
import org.springframework.data.repository.CrudRepository;

public interface RolJpaRepository extends CrudRepository<RolEntity, Integer> {
}
