package co.com.crediya.jpa;

import co.com.crediya.jpa.entity.UserEntity;
import co.com.crediya.jpa.helper.AdapterOperations;
import co.com.crediya.model.exceptions.CorreoYaRegistradoException;
import co.com.crediya.model.user.User;
import co.com.crediya.model.user.gateways.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Adaptador de salida: implementa el puerto UserRepository del dominio usando JPA.
 */
@Slf4j
@Repository
public class JPARepositoryAdapter extends AdapterOperations<User, UserEntity, Long, JPARepository>
        implements UserRepository {

    public JPARepositoryAdapter(JPARepository repository, ObjectMapper mapper) {
        super(repository, mapper, JPARepositoryAdapter::toDomain);
    }

    @Override
    @Transactional
    public User save(User user) {
        log.debug("Guardando usuario con correo {} en la base de datos", user.getCorreoElectronico());
        try {
            User guardado = super.save(user);
            log.info("Usuario persistido con id {}", guardado.getId());
            return guardado;
        } catch (DataIntegrityViolationException e) {
            // Protege el caso en que dos peticiones con el mismo correo llegan al mismo tiempo
            if (esViolacionDeCorreoUnico(e)) {
                log.warn("Correo duplicado detectado por la base de datos: {}", user.getCorreoElectronico());
                throw new CorreoYaRegistradoException(user.getCorreoElectronico());
            }
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCorreoElectronico(String correoElectronico) {
        return repository.existsByCorreoElectronico(correoElectronico);
    }

    @Override
    protected UserEntity toData(User user) {
        return UserEntity.builder()
                .id(user.getId())
                .nombres(user.getNombres())
                .apellidos(user.getApellidos())
                .fechaNacimiento(user.getFechaNacimiento())
                .direccion(user.getDireccion())
                .telefono(user.getTelefono())
                .correoElectronico(user.getCorreoElectronico())
                .salarioBase(user.getSalarioBase())
                .build();
    }

    private static User toDomain(UserEntity entity) {
        return User.builder()
                .id(entity.getId())
                .nombres(entity.getNombres())
                .apellidos(entity.getApellidos())
                .fechaNacimiento(entity.getFechaNacimiento())
                .direccion(entity.getDireccion())
                .telefono(entity.getTelefono())
                .correoElectronico(entity.getCorreoElectronico())
                .salarioBase(entity.getSalarioBase())
                .build();
    }

    private static boolean esViolacionDeCorreoUnico(DataIntegrityViolationException e) {
        String mensaje = String.valueOf(e.getMostSpecificCause().getMessage()).toLowerCase(Locale.ROOT);
        return mensaje.contains(UserEntity.UK_CORREO_ELECTRONICO);
    }
}
