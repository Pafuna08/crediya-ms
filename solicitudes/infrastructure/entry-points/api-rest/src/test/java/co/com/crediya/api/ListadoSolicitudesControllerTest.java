package co.com.crediya.api;

import co.com.crediya.model.cliente.Cliente;
import co.com.crediya.model.estado.Estado;
import co.com.crediya.model.exceptions.DatosInvalidosException;
import co.com.crediya.model.exceptions.ServicioExternoException;
import co.com.crediya.model.pagina.Pagina;
import co.com.crediya.model.solicitud.FiltroSolicitudes;
import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.model.solicitud.SolicitudParaRevision;
import co.com.crediya.model.tipoprestamo.TipoPrestamo;
import co.com.crediya.usecase.listarsolicitudes.ListarSolicitudesUseCase;
import co.com.crediya.usecase.solicitud.SolicitudUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest(classes = TestConfig.class, properties = TokensDePrueba.PROPIEDAD_SECRETO)
class ListadoSolicitudesControllerTest {

    private static final String URL = "/api/v1/solicitud";
    private static final String ASESOR = TokensDePrueba.bearer("ASESOR", "2000000000");

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

    private RestTestClient.ResponseSpec get(String query, String authorization) {
        RestTestClient.RequestHeadersSpec<?> spec = client.get().uri(URL + query);
        if (authorization != null) {
            spec.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        return spec.exchange();
    }

    private static SolicitudParaRevision fila() {
        Solicitud solicitud = Solicitud.builder()
                .id(7L).documentoIdentidad("3000000000").monto(new BigDecimal("5000000")).plazo(24)
                .tipoPrestamo(TipoPrestamo.builder().id(1L).nombre("Libre inversión").tasaInteres(new BigDecimal("1.80")).build())
                .estado(Estado.builder().id(1L).nombre(Estado.PENDIENTE_REVISION).build())
                .fechaCreacion(LocalDateTime.of(2026, 11, 6, 9, 0))
                .build();
        Cliente carlos = Cliente.builder().documentoIdentidad("3000000000").nombres("Carlos").apellidos("Cliente")
                .correoElectronico("carlos@correo.com").salarioBase(new BigDecimal("2500000")).build();
        return new SolicitudParaRevision(solicitud, carlos, new BigDecimal("88848.79"));
    }

    @Test
    void asesorObtieneElListadoConTodosLosCamposDeLaHistoria() {
        when(listarSolicitudesUseCase.listar(FiltroSolicitudes.sinFiltros(), 0, 10))
                .thenReturn(new Pagina<>(List.of(fila()), 0, 10, 21));

        get("", ASESOR)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.pagina").isEqualTo(0)
                .jsonPath("$.tamano").isEqualTo(10)
                .jsonPath("$.total_elementos").isEqualTo(21)
                .jsonPath("$.total_paginas").isEqualTo(3)
                .jsonPath("$.contenido[0].monto").isEqualTo(5000000)
                .jsonPath("$.contenido[0].plazo").isEqualTo(24)
                .jsonPath("$.contenido[0].email").isEqualTo("carlos@correo.com")
                .jsonPath("$.contenido[0].nombre").isEqualTo("Carlos Cliente")
                .jsonPath("$.contenido[0].tipo_prestamo").isEqualTo("Libre inversión")
                .jsonPath("$.contenido[0].tasa_interes").isEqualTo(1.80)
                .jsonPath("$.contenido[0].estado_solicitud").isEqualTo("Pendiente de revisión")
                .jsonPath("$.contenido[0].salario_base").isEqualTo(2500000)
                .jsonPath("$.contenido[0].deuda_total_mensual_solicitudes_aprobadas").isEqualTo(88848.79);
    }

    @Test
    void pasaLosFiltrosYLaPaginacionAlCasoDeUso() {
        when(listarSolicitudesUseCase.listar(any(), anyInt(), anyInt())).thenReturn(new Pagina<>(List.of(), 2, 5, 0));

        get("?pagina=2&tamano=5&estado=Rechazada&id_tipo_prestamo=3&documento_identidad=3000000000", ASESOR)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.contenido.length()").isEqualTo(0);

        verify(listarSolicitudesUseCase).listar(new FiltroSolicitudes("Rechazada", 3L, "3000000000"), 2, 5);
    }

    @Test
    void clienteYAdminNoPuedenVerElListado() {
        get("", TokensDePrueba.bearer("CLIENTE", "3000000000"))
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.mensajes[0]").isEqualTo("No tiene permisos para realizar esta acción");
        get("", TokensDePrueba.bearer("ADMIN", "1000000000")).expectStatus().isForbidden();

        verify(listarSolicitudesUseCase, never()).listar(any(), anyInt(), anyInt());
    }

    @Test
    void sinTokenRespondeUnauthorized() {
        get("", null).expectStatus().isUnauthorized();
    }

    @Test
    void respondeBadRequestConParametrosInvalidos() {
        when(listarSolicitudesUseCase.listar(any(), eq(0), eq(500))).thenThrow(new DatosInvalidosException(
                List.of("El parámetro tamano debe estar entre 1 y 50")));

        get("?tamano=500", ASESOR)
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.mensajes[0]").isEqualTo("El parámetro tamano debe estar entre 1 y 50");
        get("?pagina=primera", ASESOR)
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.mensajes[0]").isEqualTo("El parámetro pagina debe ser un número entero");
    }

    @Test
    void siAutenticacionNoRespondeDevuelveServiceUnavailable() {
        when(listarSolicitudesUseCase.listar(any(), anyInt(), anyInt())).thenThrow(new ServicioExternoException(
                "No fue posible consultar la información de los clientes", new RuntimeException("Connection refused")));

        get("", ASESOR)
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.mensajes[0]").isEqualTo(
                        "No fue posible consultar la información de los clientes. Intente nuevamente más tarde");
    }
}
