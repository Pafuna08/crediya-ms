package co.com.crediya.usecase.consultarusuarios;

import co.com.crediya.model.exceptions.DatosInvalidosException;
import co.com.crediya.model.user.User;
import co.com.crediya.model.user.gateways.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultarUsuariosUseCaseTest {

    private UserRepository userRepository;
    private ConsultarUsuariosUseCase useCase;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        useCase = new ConsultarUsuariosUseCase(userRepository);
    }

    @Test
    void limpiaYQuitaDuplicadosAntesDeConsultar() {
        User carlos = User.builder().documentoIdentidad("3000000000").build();
        when(userRepository.findByDocumentosIdentidad(List.of("3000000000", "4000000000"))).thenReturn(List.of(carlos));

        List<User> resultado = useCase.buscarPorDocumentos(
                Arrays.asList(" 3000000000 ", "4000000000", "3000000000", "", null));

        assertEquals(List.of(carlos), resultado);
    }

    @Test
    void rechazaListaVaciaONula() {
        assertThrows(DatosInvalidosException.class, () -> useCase.buscarPorDocumentos(List.of()));
        assertThrows(DatosInvalidosException.class, () -> useCase.buscarPorDocumentos(null));
        assertThrows(DatosInvalidosException.class, () -> useCase.buscarPorDocumentos(List.of(" ")));
        verify(userRepository, never()).findByDocumentosIdentidad(any());
    }

    @Test
    void rechazaMasDeCienDocumentos() {
        List<String> muchos = new ArrayList<>(IntStream.rangeClosed(1, 101).mapToObj(String::valueOf).toList());

        DatosInvalidosException ex = assertThrows(DatosInvalidosException.class,
                () -> useCase.buscarPorDocumentos(muchos));

        assertEquals("No se pueden consultar más de 100 documentos por petición", ex.getErrores().getFirst());
    }
}
