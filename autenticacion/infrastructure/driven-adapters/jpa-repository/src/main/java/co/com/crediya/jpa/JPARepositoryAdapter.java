package co.com.crediya.jpa;

import co.com.crediya.jpa.entity.UserEntity;
import co.com.crediya.jpa.helper.AdapterOperations;
import co.com.crediya.model.exceptions.CorreoYaRegistradoException;
import co.com.crediya.model.exceptions.DocumentoYaRegistradoException;
import co.com.crediya.model.rol.Rol;
import co.com.crediya.model.user.User;
import co.com.crediya.model.user.gateways.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Adaptador de salida: implementa el puerto UserRepository del dominio usando JPA.
 */
@Slf4j
@Repository
public class JPARepositoryAdapter extends AdapterOperations<User, UserEntity, Integer, JPARepository>
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
            // La tabla solo guarda el id del rol; se conserva el rol completo que ya trae el dominio
            return guardado.toBuilder().rol(user.getRol()).build();
        } catch (DataIntegrityViolationException e) {
            // Protege el caso en que dos peticiones con el mismo correo o documento llegan al mismo tiempo
            String mensaje = String.valueOf(e.getMostSpecificCause().getMessage()).toLowerCase(Locale.ROOT);
            if (mensaje.contains(UserEntity.UK_EMAIL)) {
                log.warn("Correo duplicado detectado por la base de datos: {}", user.getCorreoElectronico());
                throw new CorreoYaRegistradoException(user.getCorreoElectronico());
            }
            if (mensaje.contains(UserEntity.UK_DOCUMENTO)) {
                log.warn("Documento duplicado detectado por la base de datos: {}", user.getDocumentoIdentidad());
                throw new DocumentoYaRegistradoException(user.getDocumentoIdentidad());
            }
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCorreoElectronico(String correoElectronico) {
        return repository.existsByEmail(correoElectronico);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByDocumentoIdentidad(String documentoIdentidad) {
        return repository.existsByDocumentoIdentidad(documentoIdentidad);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByCorreoElectronico(String correoElectronico) {
        return repository.findByEmail(correoElectronico).map(JPARepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findByDocumentosIdentidad(List<String> documentosIdentidad) {
        return repository.findByDocumentoIdentidadIn(documentosIdentidad).stream()
                .map(JPARepositoryAdapter::toDomain)
                .toList();
    }

    /** Dominio → tabla "usuario" (los nombres de columna difieren de los del dominio). */
    @Override
    protected UserEntity toData(User user) {
        return UserEntity.builder()
                .idUsuario(user.getId() == null ? null : Math.toIntExact(user.getId()))
                .nombre(user.getNombres())
                .apellido(user.getApellidos())
                .documentoIdentidad(user.getDocumentoIdentidad())
                .fechaNacimiento(user.getFechaNacimiento())
                .direccion(user.getDireccion())
                .telefono(user.getTelefono())
                .email(user.getCorreoElectronico())
                .salarioBase(user.getSalarioBase())
                .clave(user.getClave())
                .idRol(user.getRol() == null || user.getRol().getId() == null
                        ? null : Math.toIntExact(user.getRol().getId()))
                .build();
    }

    /** Tabla "usuario" → dominio. */
    private static User toDomain(UserEntity entity) {
        Rol rol = entity.getRol() != null
                ? RolRepositoryAdapter.toDomain(entity.getRol())
                : (entity.getIdRol() == null ? null : Rol.builder().id(entity.getIdRol().longValue()).build());
        return User.builder()
                .id(entity.getIdUsuario() == null ? null : entity.getIdUsuario().longValue())
                .nombres(entity.getNombre())
                .apellidos(entity.getApellido())
                .documentoIdentidad(entity.getDocumentoIdentidad())
                .fechaNacimiento(entity.getFechaNacimiento())
                .direccion(entity.getDireccion())
                .telefono(entity.getTelefono())
                .correoElectronico(entity.getEmail())
                .salarioBase(entity.getSalarioBase())
                .clave(entity.getClave())
                .rol(rol)
                .build();
    }
}
