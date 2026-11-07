package co.com.crediya.usecase.listarsolicitudes;

import co.com.crediya.model.cliente.Cliente;
import co.com.crediya.model.cliente.gateways.ClienteGateway;
import co.com.crediya.model.estado.Estado;
import co.com.crediya.model.exceptions.DatosInvalidosException;
import co.com.crediya.model.pagina.Pagina;
import co.com.crediya.model.solicitud.FiltroSolicitudes;
import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.model.solicitud.SolicitudParaRevision;
import co.com.crediya.model.solicitud.gateways.SolicitudRepository;
import co.com.crediya.model.tipoprestamo.TipoPrestamo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListarSolicitudesUseCaseTest {

    private static final TipoPrestamo AL_1_POR_CIENTO =
            TipoPrestamo.builder().id(3L).nombre("Vivienda").tasaInteres(new BigDecimal("1.00")).build();

    private SolicitudRepository solicitudRepository;
    private ClienteGateway clienteGateway;
    private ListarSolicitudesUseCase useCase;

    @BeforeEach
    void setUp() {
        solicitudRepository = mock(SolicitudRepository.class);
        clienteGateway = mock(ClienteGateway.class);
        useCase = new ListarSolicitudesUseCase(solicitudRepository, clienteGateway);
    }

    private static Solicitud solicitud(long id, String documento, String monto, int plazo, String estado) {
        return Solicitud.builder().id(id).documentoIdentidad(documento).monto(new BigDecimal(monto)).plazo(plazo)
                .tipoPrestamo(AL_1_POR_CIENTO).estado(Estado.builder().nombre(estado).build()).build();
    }

    @Test
    void combinaSolicitudesClientesYDeudaMensualDeAprobadas() {
        Solicitud deCarlos = solicitud(1, "3000000000", "5000000", 24, Estado.PENDIENTE_REVISION);
        Solicitud deAna = solicitud(2, "4000000000", "2000000", 12, Estado.REVISION_MANUAL);
        when(solicitudRepository.findByEstados(eq(Estado.ESTADOS_PARA_REVISION), any(), eq(0), eq(10)))
                .thenReturn(new Pagina<>(List.of(deCarlos, deAna), 0, 10, 2));
        when(clienteGateway.findByDocumentos(List.of("3000000000", "4000000000"))).thenReturn(List.of(
                Cliente.builder().documentoIdentidad("3000000000").nombres("Carlos").apellidos("Cliente")
                        .correoElectronico("carlos@correo.com").salarioBase(new BigDecimal("2500000")).build()));
        // Carlos ya tiene dos préstamos aprobados de 1.000.000 al 1 % a 12 meses: 88.848,79 cada uno
        when(solicitudRepository.findByDocumentosAndEstado(List.of("3000000000", "4000000000"), Estado.APROBADA))
                .thenReturn(List.of(solicitud(8, "3000000000", "1000000", 12, Estado.APROBADA),
                        solicitud(9, "3000000000", "1000000", 12, Estado.APROBADA)));

        Pagina<SolicitudParaRevision> resultado = useCase.listar(FiltroSolicitudes.sinFiltros(), 0, 10);

        assertEquals(2, resultado.totalElementos());
        SolicitudParaRevision filaCarlos = resultado.contenido().get(0);
        assertEquals("carlos@correo.com", filaCarlos.cliente().getCorreoElectronico());
        assertEquals(new BigDecimal("177697.58"), filaCarlos.deudaTotalMensualSolicitudesAprobadas());

        SolicitudParaRevision filaAna = resultado.contenido().get(1);
        assertNull(filaAna.cliente(), "Si autenticación no conoce al cliente, la fila igual se muestra");
        assertEquals(BigDecimal.ZERO, filaAna.deudaTotalMensualSolicitudesAprobadas());
    }

    @Test
    void filtraPorUnSoloEstadoAunqueVengaConOtrasMayusculas() {
        when(solicitudRepository.findByEstados(any(), any(), anyInt(), anyInt()))
                .thenReturn(new Pagina<>(List.of(), 0, 5, 0));

        useCase.listar(new FiltroSolicitudes("  RECHAZADA ", 2L, " 3000000000 "), 1, 5);

        verify(solicitudRepository).findByEstados(List.of(Estado.RECHAZADA),
                new FiltroSolicitudes(Estado.RECHAZADA, 2L, "3000000000"), 1, 5);
    }

    @Test
    void siNoHayResultadosNoConsultaAutenticacion() {
        when(solicitudRepository.findByEstados(any(), any(), anyInt(), anyInt()))
                .thenReturn(new Pagina<>(List.of(), 0, 10, 0));

        Pagina<SolicitudParaRevision> resultado = useCase.listar(null, 0, 10);

        assertTrue(resultado.contenido().isEmpty());
        verify(clienteGateway, never()).findByDocumentos(any());
    }

    @Test
    void rechazaEstadosQueNoSonDeRevision() {
        DatosInvalidosException ex = assertThrows(DatosInvalidosException.class,
                () -> useCase.listar(new FiltroSolicitudes("Aprobada", null, null), 0, 10));

        assertEquals("El parámetro estado debe ser uno de: Pendiente de revisión, Rechazada, Revision manual",
                ex.getErrores().getFirst());
        verify(solicitudRepository, never()).findByEstados(any(), any(), anyInt(), anyInt());
    }

    @Test
    void rechazaPaginacionInvalida() {
        DatosInvalidosException ex = assertThrows(DatosInvalidosException.class,
                () -> useCase.listar(null, -1, 51));

        assertEquals(2, ex.getErrores().size());
        assertThrows(DatosInvalidosException.class, () -> useCase.listar(null, 0, 0));
    }
}
