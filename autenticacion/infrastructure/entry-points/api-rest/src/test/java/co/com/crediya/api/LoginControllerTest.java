package co.com.crediya.api;

import co.com.crediya.model.exceptions.CredencialesInvalidasException;
import co.com.crediya.model.exceptions.DatosInvalidosException;
import co.com.crediya.model.seguridad.Token;
import co.com.crediya.usecase.consultarusuarios.ConsultarUsuariosUseCase;
import co.com.crediya.usecase.login.LoginUseCase;
import co.com.crediya.usecase.user.UserUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest(classes = TestConfig.class, properties = TokensDePrueba.PROPIEDAD_SECRETO)
class LoginControllerTest {

    private static final String URL = "/api/v1/login";

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @MockitoBean
    private UserUseCase userUseCase;

    @MockitoBean
    private ConsultarUsuariosUseCase consultarUsuariosUseCase;

    private RestTestClient client;

    @BeforeEach
    void setup() {
        client = RestTestClient.bindTo(MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build())
                .build();
    }

    private RestTestClient.ResponseSpec post(String body) {
        return client.post().uri(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange();
    }

    @Test
    void iniciaSesionSinNecesitarTokenYDevuelveElToken() {
        when(loginUseCase.iniciarSesion("admin@crediya.com", "Admin123*")).thenReturn(new Token("jwt-firmado", 3600));

        post("{\"correo_electronico\":\"admin@crediya.com\",\"clave\":\"Admin123*\"}")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isEqualTo("jwt-firmado")
                .jsonPath("$.tipo_token").isEqualTo("Bearer")
                .jsonPath("$.expira_en_segundos").isEqualTo(3600);

        verify(loginUseCase).iniciarSesion("admin@crediya.com", "Admin123*");
    }

    @Test
    void respondeUnauthorizedConCredencialesIncorrectas() {
        when(loginUseCase.iniciarSesion(any(), any())).thenThrow(new CredencialesInvalidasException());

        post("{\"correo_electronico\":\"admin@crediya.com\",\"clave\":\"mala\"}")
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.mensajes[0]").isEqualTo("Correo o clave incorrectos");
    }

    @Test
    void respondeBadRequestSiFaltanDatos() {
        when(loginUseCase.iniciarSesion(any(), any())).thenThrow(new DatosInvalidosException(
                List.of("El campo correo_electronico es obligatorio", "El campo clave es obligatorio")));

        post("{}")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.mensajes.length()").isEqualTo(2);
    }

    @Test
    void respondeBadRequestSiElJsonEstaMalFormado() {
        post("{ esto no es json")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400);
    }

    @Test
    void elHealthCheckEsPublico() {
        client.get().uri("/actuator/health").exchange().expectStatus().value(status ->
                org.junit.jupiter.api.Assertions.assertNotEquals(401, status));
    }
}
