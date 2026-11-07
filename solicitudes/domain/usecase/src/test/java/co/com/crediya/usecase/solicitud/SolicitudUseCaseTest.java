package co.com.crediya.usecase.solicitud;

import co.com.crediya.model.estado.Estado;
import co.com.crediya.model.estado.gateways.EstadoRepository;
import co.com.crediya.model.exceptions.AccesoDenegadoException;
import co.com.crediya.model.exceptions.DatosInvalidosException;
import co.com.crediya.model.exceptions.TipoPrestamoNoExisteException;
import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.model.solicitud.gateways.SolicitudRepository;
import co.com.crediya.model.tipoprestamo.TipoPrestamo;
import co.com.crediya.model.tipoprestamo.gateways.TipoPrestamoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SolicitudUseCaseTest {

    private static final TipoPrestamo LIBRE_INVERSION =
            TipoPrestamo.builder().id(1L).nombre("Libre inversión").tasaInteres(new BigDecimal("1.80")).build();
    private static final Estado PENDIENTE =
            Estado.builder().id(1L).nombre(Estado.PENDIENTE_REVISION).build();

    private static final String DOC = "1234567890";

    private SolicitudRepository solicitudRepository;
    private TipoPrestamoRepository tipoPrestamoRepository;
    private EstadoRepository estadoRepository;
    private SolicitudUseCase useCase;

    @BeforeEach
    void setUp() {
        solicitudRepository = mock(SolicitudRepository.class);
        tipoPrestamoRepository = mock(TipoPrestamoRepository.class);
        estadoRepository = mock(EstadoRepository.class);
        useCase = new SolicitudUseCase(solicitudRepository, tipoPrestamoRepository, estadoRepository);

        when(tipoPrestamoRepository.findById(1L)).thenReturn(Optional.of(LIBRE_INVERSION));
        when(estadoRepository.findByNombre(Estado.PENDIENTE_REVISION)).thenReturn(Optional.of(PENDIENTE));
        when(solicitudRepository.save(any(Solicitud.class)))
                .thenAnswer(inv -> inv.<Solicitud>getArgument(0).toBuilder().id(10L).build());
    }

    private Solicitud solicitudValida() {
        return Solicitud.builder()
                .documentoIdentidad("1234567890")
                .monto(new BigDecimal("5000000"))
                .plazo(24)
                .tipoPrestamo(TipoPrestamo.builder().id(1L).build())
                .build();
    }

    @Test
    void registraSolicitudConEstadoPendienteDeRevision() {
        Solicitud entrada = solicitudValida().toBuilder()
                .id(99L)
                .documentoIdentidad(" 1234567890 ")
                .estado(Estado.builder().id(4L).nombre("Aprobada").build())
                .build();

        Solicitud resultado = useCase.registrarSolicitud(entrada, DOC);

        ArgumentCaptor<Solicitud> captor = ArgumentCaptor.forClass(Solicitud.class);
        verify(solicitudRepository).save(captor.capture());
        Solicitud guardada = captor.getValue();
        assertNull(guardada.getId(), "El id lo asigna la base de datos");
        assertEquals("1234567890", guardada.getDocumentoIdentidad());
        assertEquals(Estado.PENDIENTE_REVISION, guardada.getEstado().getNombre(),
                "El cliente no puede elegir el estado: siempre inicia pendiente de revisión");
        assertEquals("Libre inversión", guardada.getTipoPrestamo().getNombre());
        assertEquals(10L, resultado.getId());
    }

    @Test
    void rechazaTipoDePrestamoInexistente() {
        when(tipoPrestamoRepository.findById(77L)).thenReturn(Optional.empty());
        Solicitud entrada = solicitudValida().toBuilder().tipoPrestamo(TipoPrestamo.builder().id(77L).build()).build();

        TipoPrestamoNoExisteException ex = assertThrows(TipoPrestamoNoExisteException.class,
                () -> useCase.registrarSolicitud(entrada, DOC));

        assertTrue(ex.getMessage().contains("77"));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void rechazaCamposObligatoriosVacios() {
        Solicitud vacia = Solicitud.builder().documentoIdentidad("  ").build();

        DatosInvalidosException ex = assertThrows(DatosInvalidosException.class,
                () -> useCase.registrarSolicitud(vacia, DOC));

        assertEquals(4, ex.getErrores().size());
        assertTrue(ex.getErrores().contains("El campo documento_identidad es obligatorio"));
        assertTrue(ex.getErrores().contains("El campo monto es obligatorio"));
        assertTrue(ex.getErrores().contains("El campo plazo es obligatorio"));
        assertTrue(ex.getErrores().contains("El campo id_tipo_prestamo es obligatorio"));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void rechazaSolicitudNula() {
        assertThrows(DatosInvalidosException.class, () -> useCase.registrarSolicitud(null, DOC));
    }

    @Test
    void rechazaMontoYPlazoNoPositivos() {
        Solicitud entrada = solicitudValida().toBuilder().monto(BigDecimal.ZERO).plazo(-3).build();

        DatosInvalidosException ex = assertThrows(DatosInvalidosException.class,
                () -> useCase.registrarSolicitud(entrada, DOC));

        assertTrue(ex.getErrores().contains("El campo monto debe ser mayor que 0"));
        assertTrue(ex.getErrores().contains("El campo plazo debe ser mayor que 0 (en meses)"));
    }

    @Test
    void rechazaDocumentoConFormatoInvalido() {
        Solicitud conSimbolos = solicitudValida().toBuilder().documentoIdentidad("123-456").build();
        Solicitud muyLargo = solicitudValida().toBuilder().documentoIdentidad("1".repeat(21)).build();

        DatosInvalidosException ex1 = assertThrows(DatosInvalidosException.class,
                () -> useCase.registrarSolicitud(conSimbolos, DOC));
        DatosInvalidosException ex2 = assertThrows(DatosInvalidosException.class,
                () -> useCase.registrarSolicitud(muyLargo, DOC));

        assertTrue(ex1.getErrores().getFirst().contains("letras y números"));
        assertTrue(ex2.getErrores().getFirst().contains("20 caracteres"));
    }

    @Test
    void fallaControladamenteSiNoExisteElEstadoInicial() {
        when(estadoRepository.findByNombre(Estado.PENDIENTE_REVISION)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> useCase.registrarSolicitud(solicitudValida(), DOC));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void clienteNoPuedeCrearSolicitudesAOtroDocumento() {
        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> useCase.registrarSolicitud(solicitudValida(), "9999999999"));

        assertEquals("Solo puede crear solicitudes de préstamo a su propio nombre", ex.getMessage());
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void sinDocumentoDelSolicitanteNoSePermiteCrearSolicitudes() {
        assertThrows(AccesoDenegadoException.class, () -> useCase.registrarSolicitud(solicitudValida(), null));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void losDatosInvalidosSeInformanAntesQueElPermiso() {
        Solicitud sinMonto = solicitudValida().toBuilder().monto(null).build();

        assertThrows(DatosInvalidosException.class, () -> useCase.registrarSolicitud(sinMonto, "9999999999"));
    }
}
