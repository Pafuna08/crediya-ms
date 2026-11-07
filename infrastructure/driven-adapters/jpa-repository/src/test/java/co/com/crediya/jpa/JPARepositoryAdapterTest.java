package co.com.crediya.jpa;

import co.com.crediya.jpa.entity.UserEntity;
import co.com.crediya.model.exceptions.CorreoYaRegistradoException;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JPARepositoryAdapterTest {

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
                .fechaNacimiento(LocalDate.of(2000, 1, 15))
                .direccion("Calle 1")
                .telefono("3001234567")
                .correoElectronico("pablo@correo.com")
                .salarioBase(new BigDecimal("2500000"))
                .build();
    }

    @Test
    void guardaUsuarioMapeandoTodosLosCampos() {
        when(repository.save(any(UserEntity.class))).thenAnswer(inv -> {
            UserEntity e = inv.getArgument(0);
            e.setIdUsuario(1);
            return e;
        });

        User resultado = adapter.save(usuario());

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(repository).save(captor.capture());
        UserEntity enviado = captor.getValue();
        assertEquals("Pablo", enviado.getNombre());
        assertEquals("Fuenmayor", enviado.getApellido());
        assertEquals(LocalDate.of(2000, 1, 15), enviado.getFechaNacimiento());
        assertEquals("Calle 1", enviado.getDireccion());
        assertEquals("3001234567", enviado.getTelefono());
        assertEquals("pablo@correo.com", enviado.getEmail());
        assertEquals(new BigDecimal("2500000"), enviado.getSalarioBase());

        assertEquals(1L, resultado.getId());
        assertEquals("pablo@correo.com", resultado.getCorreoElectronico());
    }

    @Test
    void traduceViolacionDeCorreoUnicoAExcepcionDeNegocio() {
        when(repository.save(any(UserEntity.class))).thenThrow(new DataIntegrityViolationException(
                "Unique index or primary key violation: \"usuario_email_key\""));

        assertThrows(CorreoYaRegistradoException.class, () -> adapter.save(usuario()));
    }

    @Test
    void propagaOtrasViolacionesDeIntegridad() {
        DataIntegrityViolationException otra = new DataIntegrityViolationException("null value in column \"nombre\" violates not-null constraint");
        when(repository.save(any(UserEntity.class))).thenThrow(otra);

        DataIntegrityViolationException lanzada =
                assertThrows(DataIntegrityViolationException.class, () -> adapter.save(usuario()));
        assertSame(otra, lanzada);
    }

    @Test
    void consultaSiElCorreoExiste() {
        when(repository.existsByEmail("pablo@correo.com")).thenReturn(true);

        assertTrue(adapter.existsByCorreoElectronico("pablo@correo.com"));
    }

    @Test
    void buscaPorIdYListaTodos() {
        UserEntity entidad = UserEntity.builder().idUsuario(5).nombre("Ana").email("ana@correo.com").build();
        when(repository.findById(5)).thenReturn(Optional.of(entidad));
        when(repository.findAll()).thenReturn(List.of(entidad));

        assertEquals("Ana", adapter.findById(5).getNombres());
        assertEquals(1, adapter.findAll().size());
    }
}
