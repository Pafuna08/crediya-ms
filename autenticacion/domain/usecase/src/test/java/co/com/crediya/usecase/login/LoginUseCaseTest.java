package co.com.crediya.usecase.login;

import co.com.crediya.model.exceptions.CredencialesInvalidasException;
import co.com.crediya.model.exceptions.DatosInvalidosException;
import co.com.crediya.model.rol.Rol;
import co.com.crediya.model.seguridad.Token;
import co.com.crediya.model.seguridad.gateways.CifradorClaves;
import co.com.crediya.model.seguridad.gateways.GeneradorToken;
import co.com.crediya.model.user.User;
import co.com.crediya.model.user.gateways.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginUseCaseTest {

    private static final User ASESORA = User.builder()
            .id(5L)
            .correoElectronico("asesora@crediya.com")
            .clave("HASH")
            .rol(Rol.builder().id(2L).nombre(Rol.ASESOR).build())
            .build();

    private UserRepository userRepository;
    private CifradorClaves cifradorClaves;
    private GeneradorToken generadorToken;
    private LoginUseCase useCase;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        cifradorClaves = mock(CifradorClaves.class);
        generadorToken = mock(GeneradorToken.class);
        useCase = new LoginUseCase(userRepository, cifradorClaves, generadorToken);

        when(userRepository.findByCorreoElectronico("asesora@crediya.com")).thenReturn(Optional.of(ASESORA));
        when(cifradorClaves.coincide("ClaveCorrecta1", "HASH")).thenReturn(true);
    }

    @Test
    void emiteTokenConCredencialesCorrectasYNormalizaElCorreo() {
        Token esperado = new Token("token-firmado", 3600);
        when(generadorToken.generar(ASESORA)).thenReturn(esperado);

        Token token = useCase.iniciarSesion("  Asesora@CrediYa.com ", "ClaveCorrecta1");

        assertSame(esperado, token);
    }

    @Test
    void rechazaClaveIncorrectaSinGenerarToken() {
        CredencialesInvalidasException ex = assertThrows(CredencialesInvalidasException.class,
                () -> useCase.iniciarSesion("asesora@crediya.com", "ClaveErrada"));

        assertEquals("Correo o clave incorrectos", ex.getMessage());
        verify(generadorToken, never()).generar(any());
    }

    @Test
    void rechazaCorreoInexistenteConElMismoMensaje() {
        CredencialesInvalidasException ex = assertThrows(CredencialesInvalidasException.class,
                () -> useCase.iniciarSesion("nadie@crediya.com", "ClaveCorrecta1"));

        assertEquals("Correo o clave incorrectos", ex.getMessage());
    }

    @Test
    void permiteIntentosIlimitados() {
        for (int i = 0; i < 20; i++) {
            assertThrows(CredencialesInvalidasException.class,
                    () -> useCase.iniciarSesion("asesora@crediya.com", "ClaveErrada"));
        }
        when(generadorToken.generar(ASESORA)).thenReturn(new Token("ok", 3600));

        assertEquals("ok", useCase.iniciarSesion("asesora@crediya.com", "ClaveCorrecta1").valor(),
                "Tras muchos intentos fallidos el usuario sigue pudiendo entrar");
    }

    @Test
    void rechazaDatosVacios() {
        DatosInvalidosException ex = assertThrows(DatosInvalidosException.class,
                () -> useCase.iniciarSesion(" ", null));

        assertEquals(2, ex.getErrores().size());
        verify(userRepository, never()).findByCorreoElectronico(any());
    }
}
