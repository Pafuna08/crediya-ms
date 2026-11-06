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
            e.setId(1L);
            return e;
        });

        User resultado = adapter.save(usuario());

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(repository).save(captor.capture());
        UserEntity enviado = captor.getValue();
        assertEquals("Pablo", enviado.getNombres());
        assertEquals("Fuenmayor", enviado.getApellidos());
        assertEquals(LocalDate.of(2000, 1, 15), enviado.getFechaNacimiento());
        assertEquals("Calle 1", enviado.getDireccion());
        assertEquals("3001234567", enviado.getTelefono());
        assertEquals("pablo@correo.com", enviado.getCorreoElectronico());
        assertEquals(new BigDecimal("2500000"), enviado.getSalarioBase());

        assertEquals(1L, resultado.getId());
        assertEquals("pablo@correo.com", resultado.getCorreoElectronico());
    }

    @Test
    void traduceViolacionDeCorreoUnicoAExcepcionDeNegocio() {
        when(repository.save(any(UserEntity.class))).thenThrow(new DataIntegrityViolationException(
                "Unique index or primary key violation: \"PUBLIC.UK_USUARIOS_CORREO_ELECTRONICO\""));

        assertThrows(CorreoYaRegistradoException.class, () -> adapter.save(usuario()));
    }

    @Test
    void propagaOtrasViolacionesDeIntegridad() {
        DataIntegrityViolationException otra = new DataIntegrityViolationException("NULL not allowed for column NOMBRES");
        when(repository.save(any(UserEntity.class))).thenThrow(otra);

        DataIntegrityViolationException lanzada =
                assertThrows(DataIntegrityViolationException.class, () -> adapter.save(usuario()));
        assertSame(otra, lanzada);
    }

    @Test
    void consultaSiElCorreoExiste() {
        when(repository.existsByCorreoElectronico("pablo@correo.com")).thenReturn(true);

        assertTrue(adapter.existsByCorreoElectronico("pablo@correo.com"));
    }

    @Test
    void buscaPorIdYListaTodos() {
        UserEntity entidad = UserEntity.builder().id(5L).nombres("Ana").correoElectronico("ana@correo.com").build();
        when(repository.findById(5L)).thenReturn(Optional.of(entidad));
        when(repository.findAll()).thenReturn(List.of(entidad));

        assertEquals("Ana", adapter.findById(5L).getNombres());
        assertEquals(1, adapter.findAll().size());
    }
}
