package co.com.crediya.api;

import co.com.crediya.model.estado.Estado;
import co.com.crediya.model.exceptions.AccesoDenegadoException;
import co.com.crediya.model.exceptions.DatosInvalidosException;
import co.com.crediya.model.exceptions.TipoPrestamoNoExisteException;
import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.model.tipoprestamo.TipoPrestamo;
import co.com.crediya.usecase.listarsolicitudes.ListarSolicitudesUseCase;
import co.com.crediya.usecase.solicitud.SolicitudUseCase;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest(classes = TestConfig.class, properties = TokensDePrueba.PROPIEDAD_SECRETO)
class SolicitudControllerTest {

    private static final String URL = "/api/v1/solicitud";
    private static final String DOC = "1234567890";
    private static final String CLIENTE = TokensDePrueba.bearer("CLIENTE", DOC);
    private static final String BODY_VALIDO = """
            {
              "documento_identidad": "1234567890",
              "monto": 5000000,
              "plazo": 24,
              "id_tipo_prestamo": 1
            }
            """;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private SolicitudUseCase solicitudUseCase;

    @MockitoBean
    private ListarSolicitudesUseCase listarSolicitudesUseCase;

    private RestTestClient client;

    @BeforeEach
    void setup() {
        client = RestTestClient.bindTo(MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build())
                .build();
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
    void clienteRegistraSolicitudYRespondeCreatedConEstadoPendiente() {
        when(solicitudUseCase.registrarSolicitud(any(Solicitud.class), eq(DOC))).thenAnswer(inv ->
                inv.<Solicitud>getArgument(0).toBuilder()
                        .id(10L)
                        .tipoPrestamo(TipoPrestamo.builder().id(1L).nombre("Libre inversión")
                                .tasaInteres(new BigDecimal("1.80")).build())
                        .estado(Estado.builder().id(1L).nombre(Estado.PENDIENTE_REVISION).build())
                        .fechaCreacion(LocalDateTime.of(2026, 11, 6, 10, 30))
                        .build());

        post(BODY_VALIDO, CLIENTE)
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id_solicitud").isEqualTo(10)
                .jsonPath("$.documento_identidad").isEqualTo("1234567890")
                .jsonPath("$.monto").isEqualTo(5000000)
                .jsonPath("$.plazo").isEqualTo(24)
                .jsonPath("$.tipo_prestamo").isEqualTo("Libre inversión")
                .jsonPath("$.tasa_interes").isEqualTo(1.80)
                .jsonPath("$.estado").isEqualTo("Pendiente de revisión")
                .jsonPath("$.fecha_creacion").isEqualTo("2026-11-06T10:30:00");

        verify(solicitudUseCase).registrarSolicitud(argThat(s ->
                s.getTipoPrestamo().getId() == 1L && s.getEstado() == null), eq(DOC));
    }

    @Test
    void elDocumentoDelSolicitanteSaleDelTokenFirmado() {
        when(solicitudUseCase.registrarSolicitud(any(Solicitud.class), eq("5555555555")))
                .thenThrow(new AccesoDenegadoException("Solo puede crear solicitudes de préstamo a su propio nombre"));

        post(BODY_VALIDO, TokensDePrueba.bearer("CLIENTE", "5555555555"))
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.mensajes[0]").isEqualTo("Solo puede crear solicitudes de préstamo a su propio nombre");
    }

    @Test
    void asesorYAdminNoPuedenCrearSolicitudes() {
        post(BODY_VALIDO, TokensDePrueba.bearer("ASESOR", DOC))
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.mensajes[0]").isEqualTo("No tiene permisos para realizar esta acción");
        post(BODY_VALIDO, TokensDePrueba.bearer("ADMIN", DOC)).expectStatus().isForbidden();

        verify(solicitudUseCase, never()).registrarSolicitud(any(), any());
    }

    @Test
    void sinTokenOConTokenInvalidoRespondeUnauthorized() {
        post(BODY_VALIDO, null)
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.mensajes[0]").isEqualTo("Debe iniciar sesión con un token válido para acceder a este recurso");
        post(BODY_VALIDO, TokensDePrueba.bearerFirmadoConOtroSecreto("CLIENTE", DOC)).expectStatus().isUnauthorized();
        post(BODY_VALIDO, TokensDePrueba.bearerVencido("CLIENTE", DOC)).expectStatus().isUnauthorized();

        verify(solicitudUseCase, never()).registrarSolicitud(any(), any());
    }

    @Test
    void ignoraElEstadoSiElClienteIntentaEnviarlo() {
        when(solicitudUseCase.registrarSolicitud(any(Solicitud.class), any()))
                .thenThrow(new DatosInvalidosException(List.of("x")));

        post(BODY_VALIDO.replace("\"plazo\": 24", "\"plazo\": 24, \"estado\": \"Aprobada\""), CLIENTE)
                .expectStatus().isBadRequest();

        verify(solicitudUseCase).registrarSolicitud(argThat(s -> s.getEstado() == null), eq(DOC));
    }

    @Test
    void respondeBadRequestConLosErroresDeValidacion() {
        when(solicitudUseCase.registrarSolicitud(any(Solicitud.class), any())).thenThrow(new DatosInvalidosException(
                List.of("El campo monto es obligatorio", "El campo plazo es obligatorio")));

        post("{}", CLIENTE)
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.mensajes.length()").isEqualTo(2)
                .jsonPath("$.path").isEqualTo(URL);
    }

    @Test
    void respondeBadRequestSiElTipoDePrestamoNoExiste() {
        when(solicitudUseCase.registrarSolicitud(any(Solicitud.class), any()))
                .thenThrow(new TipoPrestamoNoExisteException(99L));

        post(BODY_VALIDO.replace("\"id_tipo_prestamo\": 1", "\"id_tipo_prestamo\": 99"), CLIENTE)
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.mensajes[0]").isEqualTo("El tipo de préstamo con id 99 no existe");
    }

    @Test
    void respondeBadRequestSiElMontoNoEsNumerico() {
        post(BODY_VALIDO.replace("5000000", "\"cinco millones\""), CLIENTE)
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.mensajes[0]").value(String.class, m ->
                        assertEquals("El campo monto tiene un formato incorrecto: debe ser un valor numérico", m));
    }

    @Test
    void respondeBadRequestSiElPlazoNoEsEntero() {
        post(BODY_VALIDO.replace("24", "\"dos años\""), CLIENTE)
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.mensajes[0]").value(String.class, m -> assertTrue(m.contains("plazo"), m));
    }

    @Test
    void ocultaLosDetallesDeErroresInesperados() {
        when(solicitudUseCase.registrarSolicitud(any(Solicitud.class), any()))
                .thenThrow(new IllegalStateException("No está configurado el estado inicial"));

        post(BODY_VALIDO, CLIENTE)
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.mensajes[0]").isEqualTo("Ocurrió un error inesperado. Intente nuevamente más tarde");
    }

    @Test
    void respondeUnsupportedMediaTypeSiNoEsJson() {
        client.post().uri(URL)
                .header(HttpHeaders.AUTHORIZATION, CLIENTE)
                .contentType(MediaType.TEXT_PLAIN)
                .body("hola")
                .exchange()
                .expectStatus().isEqualTo(415);
    }
}
