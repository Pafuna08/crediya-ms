package co.com.crediya.api;

import co.com.crediya.model.exceptions.CorreoYaRegistradoException;
import co.com.crediya.model.exceptions.DatosInvalidosException;
import co.com.crediya.model.exceptions.DocumentoYaRegistradoException;
import co.com.crediya.model.exceptions.RolNoExisteException;
import co.com.crediya.model.rol.Rol;
import co.com.crediya.model.user.User;
import co.com.crediya.usecase.login.LoginUseCase;
import co.com.crediya.usecase.user.UserUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest(classes = TestConfig.class, properties = TokensDePrueba.PROPIEDAD_SECRETO)
class UserControllerTest {

    private static final String URL = "/api/v1/usuarios";
    private static final String BODY_VALIDO = """
            {
              "nombres": "Pablo Enrique",
              "apellidos": "Fuenmayor Osuna",
              "documento_identidad": "1234567890",
              "fecha_nacimiento": "2000-01-15",
              "direccion": "Calle 1 # 2-3",
              "telefono": "3001234567",
              "correo_electronico": "pablo@correo.com",
              "salario_base": 2500000,
              "clave": "ClaveSegura1",
              "id_rol": 3
            }
            """;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private UserUseCase userUseCase;

    @MockitoBean
    private LoginUseCase loginUseCase;

    private RestTestClient client;

    @BeforeEach
    void setup() {
        client = RestTestClient.bindTo(MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build())
                .build();
        when(userUseCase.registrarUsuario(any(User.class))).thenAnswer(inv ->
                inv.<User>getArgument(0).toBuilder()
                        .id(1L)
                        .clave("$2a$10$hashQueNuncaDebeSalir")
                        .rol(Rol.builder().id(3L).nombre(Rol.CLIENTE).build())
                        .build());
    }

    private RestTestClient.ResponseSpec post(String body, String authorization) {
        RestTestClient.RequestBodySpec spec = client.post().uri(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);
        if (authorization != null) {
            spec.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        return spec.body(body).exchange();
    }

    @Test
    void adminRegistraUsuarioSinExponerLaClave() {
        post(BODY_VALIDO, TokensDePrueba.bearer(Rol.ADMIN))
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.documento_identidad").isEqualTo("1234567890")
                .jsonPath("$.correo_electronico").isEqualTo("pablo@correo.com")
                .jsonPath("$.rol").isEqualTo(Rol.CLIENTE)
                .jsonPath("$.clave").doesNotExist();

        verify(userUseCase).registrarUsuario(org.mockito.ArgumentMatchers.argThat(u ->
                "ClaveSegura1".equals(u.getClave()) && u.getRol().getId() == 3L));
    }

    @Test
    void asesorTambienPuedeRegistrarUsuarios() {
        post(BODY_VALIDO, TokensDePrueba.bearer(Rol.ASESOR)).expectStatus().isCreated();
    }

    @Test
    void clienteNoPuedeRegistrarUsuarios() {
        post(BODY_VALIDO, TokensDePrueba.bearer(Rol.CLIENTE))
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.mensajes[0]").isEqualTo("No tiene permisos para realizar esta acción");

        verify(userUseCase, never()).registrarUsuario(any());
    }

    @Test
    void sinTokenRespondeUnauthorized() {
        post(BODY_VALIDO, null)
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.mensajes[0]").isEqualTo("Debe iniciar sesión con un token válido para acceder a este recurso");

        verify(userUseCase, never()).registrarUsuario(any());
    }

    @Test
    void rechazaTokensFalsificadosVencidosODeOtroEmisor() {
        post(BODY_VALIDO, TokensDePrueba.bearerFirmadoConOtroSecreto(Rol.ADMIN)).expectStatus().isUnauthorized();
        post(BODY_VALIDO, TokensDePrueba.bearerVencido(Rol.ADMIN)).expectStatus().isUnauthorized();
        post(BODY_VALIDO, TokensDePrueba.bearerDeOtroEmisor(Rol.ADMIN)).expectStatus().isUnauthorized();
        post(BODY_VALIDO, "Bearer esto-no-es-un-token").expectStatus().isUnauthorized();

        verify(userUseCase, never()).registrarUsuario(any());
    }

    @Test
    void respondeBadRequestConLosErroresDeValidacion() {
        when(userUseCase.registrarUsuario(any(User.class))).thenThrow(new DatosInvalidosException(
                List.of("El campo nombres es obligatorio", "El campo clave es obligatorio")));

        post("{}", TokensDePrueba.bearer(Rol.ADMIN))
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.mensajes.length()").isEqualTo(2)
                .jsonPath("$.path").isEqualTo(URL);
    }

    @Test
    void respondeConflictCuandoElCorreoOElDocumentoYaExisten() {
        when(userUseCase.registrarUsuario(any(User.class)))
                .thenThrow(new CorreoYaRegistradoException("pablo@correo.com"))
                .thenThrow(new DocumentoYaRegistradoException("1234567890"));

        post(BODY_VALIDO, TokensDePrueba.bearer(Rol.ADMIN))
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.mensajes[0]").isEqualTo("El correo electrónico pablo@correo.com ya está registrado");
        post(BODY_VALIDO, TokensDePrueba.bearer(Rol.ADMIN))
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.mensajes[0]").isEqualTo("El documento de identidad 1234567890 ya está registrado");
    }

    @Test
    void respondeBadRequestSiElRolNoExiste() {
        when(userUseCase.registrarUsuario(any(User.class))).thenThrow(new RolNoExisteException(9L));

        post(BODY_VALIDO, TokensDePrueba.bearer(Rol.ADMIN))
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.mensajes[0]").isEqualTo("El rol con id 9 no existe");
    }

    @Test
    void respondeBadRequestCuandoElSalarioNoEsNumerico() {
        post(BODY_VALIDO.replace("2500000", "\"mucho dinero\""), TokensDePrueba.bearer(Rol.ADMIN))
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.mensajes[0]").value(String.class, m -> assertTrue(m.contains("salario_base"), m));
    }

    @Test
    void ocultaLosDetallesDeErroresInesperados() {
        when(userUseCase.registrarUsuario(any(User.class)))
                .thenThrow(new IllegalStateException("conexión a BD caída: jdbc:postgresql://secreto"));

        post(BODY_VALIDO, TokensDePrueba.bearer(Rol.ADMIN))
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.mensajes[0]").isEqualTo("Ocurrió un error inesperado. Intente nuevamente más tarde");
    }

    @Test
    void respondeUnsupportedMediaTypeSiNoEsJson() {
        client.post().uri(URL)
                .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(Rol.ADMIN))
                .contentType(MediaType.TEXT_PLAIN)
                .body("hola")
                .exchange()
                .expectStatus().isEqualTo(415);
    }

    @Test
    void respondeNotFoundEnRutaInexistenteConSesionIniciada() {
        client.get().uri("/api/v1/no-existe")
                .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(Rol.ADMIN))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.mensajes[0]").isEqualTo("El recurso solicitado no existe");
    }
}
