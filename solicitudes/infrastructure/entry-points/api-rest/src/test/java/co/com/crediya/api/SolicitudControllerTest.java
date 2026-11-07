package co.com.crediya.api;

import co.com.crediya.model.estado.Estado;
import co.com.crediya.model.exceptions.DatosInvalidosException;
import co.com.crediya.model.exceptions.TipoPrestamoNoExisteException;
import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.model.tipoprestamo.TipoPrestamo;
import co.com.crediya.usecase.solicitud.SolicitudUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = TestConfig.class)
class SolicitudControllerTest {

    private static final String URL = "/api/v1/solicitud";
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
    void registraSolicitudYRespondeCreatedConEstadoPendiente() {
        when(solicitudUseCase.registrarSolicitud(any(Solicitud.class))).thenAnswer(inv ->
                inv.<Solicitud>getArgument(0).toBuilder()
                        .id(10L)
                        .tipoPrestamo(TipoPrestamo.builder().id(1L).nombre("Libre inversión")
                                .tasaInteres(new BigDecimal("1.80")).build())
                        .estado(Estado.builder().id(1L).nombre(Estado.PENDIENTE_REVISION).build())
                        .fechaCreacion(LocalDateTime.of(2026, 11, 6, 10, 30))
                        .build());

        post(BODY_VALIDO)
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
                s.getTipoPrestamo().getId() == 1L && s.getEstado() == null));
    }

    @Test
    void ignoraElEstadoSiElClienteIntentaEnviarlo() {
        when(solicitudUseCase.registrarSolicitud(any(Solicitud.class)))
                .thenThrow(new DatosInvalidosException(List.of("x")));

        post(BODY_VALIDO.replace("\"plazo\": 24", "\"plazo\": 24, \"estado\": \"Aprobada\""))
                .expectStatus().isBadRequest();

        verify(solicitudUseCase).registrarSolicitud(argThat(s -> s.getEstado() == null));
    }

    @Test
    void respondeBadRequestConLosErroresDeValidacion() {
        when(solicitudUseCase.registrarSolicitud(any(Solicitud.class))).thenThrow(new DatosInvalidosException(
                List.of("El campo monto es obligatorio", "El campo plazo es obligatorio")));

        post("{}")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.mensajes.length()").isEqualTo(2)
                .jsonPath("$.path").isEqualTo(URL);
    }

    @Test
    void respondeBadRequestSiElTipoDePrestamoNoExiste() {
        when(solicitudUseCase.registrarSolicitud(any(Solicitud.class)))
                .thenThrow(new TipoPrestamoNoExisteException(99L));

        post(BODY_VALIDO.replace("\"id_tipo_prestamo\": 1", "\"id_tipo_prestamo\": 99"))
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.mensajes[0]").isEqualTo("El tipo de préstamo con id 99 no existe");
    }

    @Test
    void respondeBadRequestSiElMontoNoEsNumerico() {
        post(BODY_VALIDO.replace("5000000", "\"cinco millones\""))
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.mensajes[0]").value(String.class, m ->
                        assertEquals("El campo monto tiene un formato incorrecto: debe ser un valor numérico", m));
    }

    @Test
    void respondeBadRequestSiElPlazoNoEsEntero() {
        post(BODY_VALIDO.replace("24", "\"dos años\""))
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.mensajes[0]").value(String.class, m -> assertTrue(m.contains("plazo"), m));
    }

    @Test
    void ocultaLosDetallesDeErroresInesperados() {
        when(solicitudUseCase.registrarSolicitud(any(Solicitud.class)))
                .thenThrow(new IllegalStateException("No está configurado el estado inicial"));

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
}
