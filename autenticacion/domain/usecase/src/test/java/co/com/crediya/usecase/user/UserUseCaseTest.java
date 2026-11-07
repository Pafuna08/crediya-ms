package co.com.crediya.usecase.user;

import co.com.crediya.model.exceptions.CorreoYaRegistradoException;
import co.com.crediya.model.exceptions.DatosInvalidosException;
import co.com.crediya.model.exceptions.DocumentoYaRegistradoException;
import co.com.crediya.model.exceptions.RolNoExisteException;
import co.com.crediya.model.rol.Rol;
import co.com.crediya.model.rol.gateways.RolRepository;
import co.com.crediya.model.seguridad.gateways.CifradorClaves;
import co.com.crediya.model.user.User;
import co.com.crediya.model.user.gateways.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserUseCaseTest {

    private static final Rol CLIENTE = Rol.builder().id(3L).nombre(Rol.CLIENTE).build();

    private UserRepository userRepository;
    private RolRepository rolRepository;
    private CifradorClaves cifradorClaves;
    private UserUseCase userUseCase;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        rolRepository = mock(RolRepository.class);
        cifradorClaves = mock(CifradorClaves.class);
        userUseCase = new UserUseCase(userRepository, rolRepository, cifradorClaves);

        when(rolRepository.findById(3L)).thenReturn(Optional.of(CLIENTE));
        when(cifradorClaves.cifrar(any())).thenAnswer(inv -> "CIFRADA(" + inv.getArgument(0) + ")");
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.<User>getArgument(0).toBuilder().id(1L).build());
    }

    private User usuarioValido() {
        return User.builder()
                .nombres("Pablo Enrique")
                .apellidos("Fuenmayor Osuna")
                .documentoIdentidad("1234567890")
                .fechaNacimiento(LocalDate.of(2000, 1, 15))
                .direccion("Calle 1 # 2-3")
                .telefono("3001234567")
                .correoElectronico("pablo@correo.com")
                .salarioBase(new BigDecimal("2500000"))
                .clave("ClaveSegura1")
                .rol(Rol.builder().id(3L).build())
                .build();
    }

    @Test
    void registraUsuarioValidoNormalizaDatosYCifraLaClave() {
        User entrada = usuarioValido().toBuilder()
                .id(99L)
                .nombres("  Pablo Enrique ")
                .documentoIdentidad(" 1234567890 ")
                .correoElectronico(" Pablo@Correo.COM ")
                .build();

        User resultado = userUseCase.registrarUsuario(entrada);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User guardado = captor.getValue();
        assertNull(guardado.getId(), "El id lo debe asignar la base de datos");
        assertEquals("Pablo Enrique", guardado.getNombres());
        assertEquals("1234567890", guardado.getDocumentoIdentidad());
        assertEquals("pablo@correo.com", guardado.getCorreoElectronico());
        assertEquals("CIFRADA(ClaveSegura1)", guardado.getClave(), "La clave nunca se guarda en texto plano");
        assertEquals(Rol.CLIENTE, guardado.getRol().getNombre());
        assertEquals(1L, resultado.getId());
    }

    @Test
    void rechazaCorreoYaRegistrado() {
        when(userRepository.existsByCorreoElectronico("pablo@correo.com")).thenReturn(true);

        assertThrows(CorreoYaRegistradoException.class, () -> userUseCase.registrarUsuario(usuarioValido()));
        verify(userRepository, never()).save(any());
    }

    @Test
    void rechazaDocumentoYaRegistrado() {
        when(userRepository.existsByDocumentoIdentidad("1234567890")).thenReturn(true);

        assertThrows(DocumentoYaRegistradoException.class, () -> userUseCase.registrarUsuario(usuarioValido()));
        verify(userRepository, never()).save(any());
    }

    @Test
    void rechazaRolInexistente() {
        when(rolRepository.findById(9L)).thenReturn(Optional.empty());
        User entrada = usuarioValido().toBuilder().rol(Rol.builder().id(9L).build()).build();

        assertThrows(RolNoExisteException.class, () -> userUseCase.registrarUsuario(entrada));
        verify(userRepository, never()).save(any());
    }

    @Test
    void rechazaCamposObligatoriosVacios() {
        User entrada = User.builder().nombres(" ").correoElectronico("").build();

        DatosInvalidosException ex = assertThrows(DatosInvalidosException.class,
                () -> userUseCase.registrarUsuario(entrada));

        assertEquals(7, ex.getErrores().size());
        assertTrue(ex.getErrores().contains("El campo nombres es obligatorio"));
        assertTrue(ex.getErrores().contains("El campo apellidos es obligatorio"));
        assertTrue(ex.getErrores().contains("El campo documento_identidad es obligatorio"));
        assertTrue(ex.getErrores().contains("El campo correo_electronico es obligatorio"));
        assertTrue(ex.getErrores().contains("El campo salario_base es obligatorio"));
        assertTrue(ex.getErrores().contains("El campo clave es obligatorio"));
        assertTrue(ex.getErrores().contains("El campo id_rol es obligatorio"));
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
    void rechazaClaveCortaYDocumentoInvalido() {
        User entrada = usuarioValido().toBuilder().clave("corta").documentoIdentidad("12-34").build();

        DatosInvalidosException ex = assertThrows(DatosInvalidosException.class,
                () -> userUseCase.registrarUsuario(entrada));

        assertTrue(ex.getErrores().contains("El campo clave debe tener al menos 8 caracteres"));
        assertTrue(ex.getErrores().contains("El campo documento_identidad solo puede contener letras y números"));
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
        userUseCase.registrarUsuario(usuarioValido().toBuilder().salarioBase(BigDecimal.ZERO).build());
        userUseCase.registrarUsuario(usuarioValido().toBuilder().salarioBase(new BigDecimal("15000000")).build());

        verify(userRepository, times(2)).save(any(User.class));
    }
}
