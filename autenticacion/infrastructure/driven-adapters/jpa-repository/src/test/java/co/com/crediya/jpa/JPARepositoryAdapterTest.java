package co.com.crediya.jpa;

import co.com.crediya.jpa.entity.RolEntity;
import co.com.crediya.jpa.entity.UserEntity;
import co.com.crediya.model.exceptions.CorreoYaRegistradoException;
import co.com.crediya.model.exceptions.DocumentoYaRegistradoException;
import co.com.crediya.model.rol.Rol;
import co.com.crediya.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JPARepositoryAdapterTest {

    private static final Rol CLIENTE = Rol.builder().id(3L).nombre(Rol.CLIENTE).build();

    private JPARepository repository;
    private JPARepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        repository = mock(JPARepository.class);
        adapter = new JPARepositoryAdapter(repository, mock(ObjectMapper.class));
    }

    private User usuario() {
        return User.builder()
                .nombres("Pablo")
                .apellidos("Fuenmayor")
                .documentoIdentidad("1234567890")
                .fechaNacimiento(LocalDate.of(2000, 1, 15))
                .direccion("Calle 1")
                .telefono("3001234567")
                .correoElectronico("pablo@correo.com")
                .salarioBase(new BigDecimal("2500000"))
                .clave("$2a$10$hash")
                .rol(CLIENTE)
                .build();
    }

    @Test
    void guardaUsuarioMapeandoTodosLosCamposYConservaElRol() {
        when(repository.save(any(UserEntity.class))).thenAnswer(inv -> {
            UserEntity e = inv.getArgument(0);
            assertNull(e.getIdUsuario(), "Un usuario nuevo debe llegar sin id");
            e.setIdUsuario(1);
            return e;
        });

        User resultado = adapter.save(usuario());

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(repository).save(captor.capture());
        UserEntity enviado = captor.getValue();
        assertEquals("Pablo", enviado.getNombre());
        assertEquals("Fuenmayor", enviado.getApellido());
        assertEquals("1234567890", enviado.getDocumentoIdentidad());
        assertEquals(LocalDate.of(2000, 1, 15), enviado.getFechaNacimiento());
        assertEquals("Calle 1", enviado.getDireccion());
        assertEquals("3001234567", enviado.getTelefono());
        assertEquals("pablo@correo.com", enviado.getEmail());
        assertEquals(new BigDecimal("2500000"), enviado.getSalarioBase());
        assertEquals("$2a$10$hash", enviado.getClave());
        assertEquals(3, enviado.getIdRol());

        assertEquals(1L, resultado.getId());
        assertEquals(Rol.CLIENTE, resultado.getRol().getNombre());
    }

    @Test
    void traduceViolacionDeCorreoUnicoAExcepcionDeNegocio() {
        when(repository.save(any(UserEntity.class))).thenThrow(new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"usuario_email_key\""));

        assertThrows(CorreoYaRegistradoException.class, () -> adapter.save(usuario()));
    }

    @Test
    void traduceViolacionDeDocumentoUnicoAExcepcionDeNegocio() {
        when(repository.save(any(UserEntity.class))).thenThrow(new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"usuario_documento_identidad_key\""));

        assertThrows(DocumentoYaRegistradoException.class, () -> adapter.save(usuario()));
    }

    @Test
    void propagaOtrasViolacionesDeIntegridad() {
        DataIntegrityViolationException otra =
                new DataIntegrityViolationException("null value in column \"nombre\" violates not-null constraint");
        when(repository.save(any(UserEntity.class))).thenThrow(otra);

        DataIntegrityViolationException lanzada =
                assertThrows(DataIntegrityViolationException.class, () -> adapter.save(usuario()));
        assertSame(otra, lanzada);
    }

    @Test
    void consultaSiElCorreoOElDocumentoExisten() {
        when(repository.existsByEmail("pablo@correo.com")).thenReturn(true);
        when(repository.existsByDocumentoIdentidad("1234567890")).thenReturn(true);

        assertTrue(adapter.existsByCorreoElectronico("pablo@correo.com"));
        assertTrue(adapter.existsByDocumentoIdentidad("1234567890"));
    }

    @Test
    void buscaPorCorreoConClaveCifradaYRolCompleto() {
        RolEntity rolAsesor = RolEntity.builder().idRol(2).nombre(Rol.ASESOR).descripcion("Asesor").build();
        when(repository.findByEmail("asesora@crediya.com")).thenReturn(Optional.of(UserEntity.builder()
                .idUsuario(5).email("asesora@crediya.com").clave("$2a$10$x").idRol(2).rol(rolAsesor).build()));

        User usuario = adapter.findByCorreoElectronico("asesora@crediya.com").orElseThrow();

        assertEquals(5L, usuario.getId());
        assertEquals("$2a$10$x", usuario.getClave());
        assertEquals(Rol.ASESOR, usuario.getRol().getNombre());
        assertTrue(adapter.findByCorreoElectronico("nadie@crediya.com").isEmpty());
    }

    @Test
    void buscaPorIdYListaTodos() {
        UserEntity entidad = UserEntity.builder().idUsuario(5).nombre("Ana").email("ana@correo.com").idRol(3).build();
        when(repository.findById(5)).thenReturn(Optional.of(entidad));
        when(repository.findAll()).thenReturn(List.of(entidad));

        assertEquals("Ana", adapter.findById(5).getNombres());
        assertEquals(3L, adapter.findById(5).getRol().getId());
        assertEquals(1, adapter.findAll().size());
    }

    @Test
    void buscaRolPorId() {
        RolJpaRepository rolRepo = mock(RolJpaRepository.class);
        when(rolRepo.findById(1)).thenReturn(Optional.of(
                RolEntity.builder().idRol(1).nombre(Rol.ADMIN).descripcion("Administrador").build()));
        RolRepositoryAdapter rolAdapter = new RolRepositoryAdapter(rolRepo);

        Rol rol = rolAdapter.findById(1L).orElseThrow();

        assertEquals(Rol.ADMIN, rol.getNombre());
        assertTrue(rolAdapter.findById(99L).isEmpty());
        assertTrue(rolAdapter.findById(null).isEmpty());
        assertTrue(rolAdapter.findById(Long.MAX_VALUE).isEmpty());
    }
}
