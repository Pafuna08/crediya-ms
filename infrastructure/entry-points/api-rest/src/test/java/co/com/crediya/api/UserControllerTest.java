package co.com.crediya.api;

import co.com.crediya.model.exceptions.CorreoYaRegistradoException;
import co.com.crediya.model.exceptions.DatosInvalidosException;
import co.com.crediya.model.user.User;
import co.com.crediya.usecase.user.UserUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = TestConfig.class)
class UserControllerTest {

    private static final String URL = "/api/v1/usuarios";
    private static final String BODY_VALIDO = """
            {
              "nombres": "Pablo Enrique",
              "apellidos": "Fuenmayor Osuna",
              "fecha_nacimiento": "2000-01-15",
              "direccion": "Calle 1 # 2-3",
              "telefono": "3001234567",
              "correo_electronico": "pablo@correo.com",
              "salario_base": 2500000
            }
            """;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private UserUseCase userUseCase;

    private RestTestClient client;

    @BeforeEach
    void setup() {
        client = RestTestClient.bindToApplicationContext(context).build();
    }

    private RestTestClient.ResponseSpec post(String body) {
        return client.post().uri(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange();
    }

    @Test
    void registraUsuarioYRespondeCreated() {
        when(userUseCase.registrarUsuario(any(User.class))).thenAnswer(inv ->
                inv.<User>getArgument(0).toBuilder().id(1L).build());

        post(BODY_VALIDO)
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.nombres").isEqualTo("Pablo Enrique")
                .jsonPath("$.apellidos").isEqualTo("Fuenmayor Osuna")
                .jsonPath("$.fecha_nacimiento").isEqualTo("2000-01-15")
                .jsonPath("$.correo_electronico").isEqualTo("pablo@correo.com")
                .jsonPath("$.salario_base").isEqualTo(2500000);
    }

    @Test
    void respondeBadRequestConLosErroresDeValidacion() {
        when(userUseCase.registrarUsuario(any(User.class))).thenThrow(new DatosInvalidosException(
                List.of("El campo nombres es obligatorio", "El campo salario_base es obligatorio")));

        post("{}")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.mensajes.length()").isEqualTo(2)
                .jsonPath("$.mensajes[0]").isEqualTo("El campo nombres es obligatorio")
                .jsonPath("$.path").isEqualTo(URL);
    }

    @Test
    void respondeConflictCuandoElCorreoYaExiste() {
        when(userUseCase.registrarUsuario(any(User.class)))
                .thenThrow(new CorreoYaRegistradoException("pablo@correo.com"));

        post(BODY_VALIDO)
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.mensajes[0]").isEqualTo("El correo electrónico pablo@correo.com ya está registrado");
    }

    @Test
    void respondeBadRequestCuandoElSalarioNoEsNumerico() {
        post(BODY_VALIDO.replace("2500000", "\"mucho dinero\""))
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.mensajes[0]").value(String.class, m ->
                        org.junit.jupiter.api.Assertions.assertTrue(m.contains("salario_base"), m));
    }

    @Test
    void respondeBadRequestCuandoElJsonEstaMalFormado() {
        post("{ esto no es json")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400);
    }

    @Test
    void ocultaLosDetallesDeErroresInesperados() {
        when(userUseCase.registrarUsuario(any(User.class)))
                .thenThrow(new IllegalStateException("conexión a BD caída: jdbc:h2:secret"));

        post(BODY_VALIDO)
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.mensajes[0]").isEqualTo("Ocurrió un error inesperado. Intente nuevamente más tarde");
    }

    @Test
    void respondeUnsupportedMediaTypeSiNoEsJson() {
        client.post().uri(URL)
                .contentType(MediaType.TEXT_PLAIN)
                .body("hola")
                .exchange()
                .expectStatus().isEqualTo(415);
    }

    @Test
    void respondeNotFoundEnRutaInexistente() {
        client.get().uri("/api/v1/no-existe")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.mensajes[0]").isEqualTo("El recurso solicitado no existe");
    }
}
