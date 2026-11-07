package co.com.crediya.usecase.user;

import co.com.crediya.model.exceptions.CorreoYaRegistradoException;
import co.com.crediya.model.exceptions.DatosInvalidosException;
import co.com.crediya.model.user.User;
import co.com.crediya.model.user.gateways.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserUseCaseTest {

    private UserRepository userRepository;
    private UserUseCase userUseCase;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userUseCase = new UserUseCase(userRepository);
    }

    private User usuarioValido() {
        return User.builder()
                .nombres("Pablo Enrique")
                .apellidos("Fuenmayor Osuna")
                .fechaNacimiento(LocalDate.of(2000, 1, 15))
                .direccion("Calle 1 # 2-3")
                .telefono("3001234567")
                .correoElectronico("pablo@correo.com")
                .salarioBase(new BigDecimal("2500000"))
                .build();
    }

    @Test
    void registraUsuarioValidoYNormalizaDatos() {
        User entrada = usuarioValido().toBuilder()
                .id(99L)
                .nombres("  Pablo Enrique ")
                .correoElectronico(" Pablo@Correo.COM ")
                .build();
        when(userRepository.existsByCorreoElectronico("pablo@correo.com")).thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.<User>getArgument(0).toBuilder().id(1L).build());

        User resultado = userUseCase.registrarUsuario(entrada);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User guardado = captor.getValue();
        assertNull(guardado.getId(), "El id lo debe asignar la base de datos");
        assertEquals("Pablo Enrique", guardado.getNombres());
        assertEquals("pablo@correo.com", guardado.getCorreoElectronico());
        assertEquals(1L, resultado.getId());
    }

    @Test
    void rechazaCorreoYaRegistrado() {
        when(userRepository.existsByCorreoElectronico("pablo@correo.com")).thenReturn(true);

        assertThrows(CorreoYaRegistradoException.class, () -> userUseCase.registrarUsuario(usuarioValido()));
        verify(userRepository, never()).save(any());
    }

    @Test
    void rechazaCamposObligatoriosVacios() {
        User entrada = User.builder().nombres(" ").apellidos(null).correoElectronico("").build();

        DatosInvalidosException ex = assertThrows(DatosInvalidosException.class,
                () -> userUseCase.registrarUsuario(entrada));

        assertEquals(4, ex.getErrores().size());
        assertTrue(ex.getErrores().contains("El campo nombres es obligatorio"));
        assertTrue(ex.getErrores().contains("El campo apellidos es obligatorio"));
        assertTrue(ex.getErrores().contains("El campo correo_electronico es obligatorio"));
        assertTrue(ex.getErrores().contains("El campo salario_base es obligatorio"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void rechazaUsuarioNulo() {
        assertThrows(DatosInvalidosException.class, () -> userUseCase.registrarUsuario(null));
    }

    @Test
    void rechazaCorreoConFormatoInvalido() {
        User entrada = usuarioValido().toBuilder().correoElectronico("pablo-sin-arroba.com").build();

        DatosInvalidosException ex = assertThrows(DatosInvalidosException.class,
                () -> userUseCase.registrarUsuario(entrada));

        assertEquals(1, ex.getErrores().size());
        assertTrue(ex.getErrores().getFirst().contains("formato de email"));
    }

    @Test
    void rechazaSalarioFueraDeRango() {
        User negativo = usuarioValido().toBuilder().salarioBase(new BigDecimal("-1")).build();
        User excedido = usuarioValido().toBuilder().salarioBase(new BigDecimal("15000000.01")).build();

        assertThrows(DatosInvalidosException.class, () -> userUseCase.registrarUsuario(negativo));
        assertThrows(DatosInvalidosException.class, () -> userUseCase.registrarUsuario(excedido));
    }

    @Test
    void aceptaSalariosEnLosLimites() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userUseCase.registrarUsuario(usuarioValido().toBuilder().salarioBase(BigDecimal.ZERO).build());
        userUseCase.registrarUsuario(usuarioValido().toBuilder().salarioBase(new BigDecimal("15000000")).build());

        verify(userRepository, org.mockito.Mockito.times(2)).save(any(User.class));
    }
}
