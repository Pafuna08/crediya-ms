package co.com.crediya.jpa;

import co.com.crediya.jpa.entity.EstadoEntity;
import co.com.crediya.jpa.entity.SolicitudEntity;
import co.com.crediya.jpa.entity.TipoPrestamoEntity;
import co.com.crediya.jpa.repository.EstadoJpaRepository;
import co.com.crediya.jpa.repository.SolicitudJpaRepository;
import co.com.crediya.jpa.repository.TipoPrestamoJpaRepository;
import co.com.crediya.model.estado.Estado;
import co.com.crediya.model.pagina.Pagina;
import co.com.crediya.model.solicitud.FiltroSolicitudes;
import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.model.tipoprestamo.TipoPrestamo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RepositoryAdaptersTest {

    private static final TipoPrestamo VEHICULO =
            TipoPrestamo.builder().id(2L).nombre("Vehículo").tasaInteres(new BigDecimal("1.30")).build();
    private static final Estado PENDIENTE =
            Estado.builder().id(1L).nombre(Estado.PENDIENTE_REVISION).build();

    @Test
    void guardaSolicitudConLlavesForaneasYConservaDatosDelDominio() {
        SolicitudJpaRepository repo = mock(SolicitudJpaRepository.class);
        SolicitudRepositoryAdapter adapter = new SolicitudRepositoryAdapter(repo, mock(ObjectMapper.class));
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 6, 10, 0);
        when(repo.save(any(SolicitudEntity.class))).thenAnswer(inv -> {
            SolicitudEntity e = inv.getArgument(0);
            assertNull(e.getIdSolicitud(), "Una solicitud nueva debe llegar sin id");
            e.setIdSolicitud(10);
            e.setFechaCreacion(ahora);
            return e;
        });

        Solicitud resultado = adapter.save(Solicitud.builder()
                .documentoIdentidad("1234567890")
                .monto(new BigDecimal("5000000"))
                .plazo(24)
                .tipoPrestamo(VEHICULO)
                .estado(PENDIENTE)
                .build());

        ArgumentCaptor<SolicitudEntity> captor = ArgumentCaptor.forClass(SolicitudEntity.class);
        verify(repo).save(captor.capture());
        SolicitudEntity enviada = captor.getValue();
        assertEquals("1234567890", enviada.getDocumentoIdentidad());
        assertEquals(new BigDecimal("5000000"), enviada.getMonto());
        assertEquals(24, enviada.getPlazo());
        assertEquals(2, enviada.getIdTipoPrestamo());
        assertEquals(1, enviada.getIdEstado());

        assertEquals(10L, resultado.getId());
        assertEquals(ahora, resultado.getFechaCreacion());
        assertEquals("Vehículo", resultado.getTipoPrestamo().getNombre());
        assertEquals(Estado.PENDIENTE_REVISION, resultado.getEstado().getNombre());
    }

    @Test
    void asignaFechaDeCreacionAlPersistirSiNoViene() {
        SolicitudEntity entidad = new SolicitudEntity();
        entidad.asignarFechaCreacion();
        assertNotNull(entidad.getFechaCreacion());
    }

    @Test
    void buscaTipoDePrestamoPorId() {
        TipoPrestamoJpaRepository repo = mock(TipoPrestamoJpaRepository.class);
        when(repo.findById(2)).thenReturn(Optional.of(
                TipoPrestamoEntity.builder().idTipoPrestamo(2).nombre("Vehículo").tasaInteres(new BigDecimal("1.30")).build()));
        TipoPrestamoRepositoryAdapter adapter = new TipoPrestamoRepositoryAdapter(repo);

        TipoPrestamo tipo = adapter.findById(2L).orElseThrow();

        assertEquals(2L, tipo.getId());
        assertEquals("Vehículo", tipo.getNombre());
        assertEquals(new BigDecimal("1.30"), tipo.getTasaInteres());
    }

    @Test
    void tipoDePrestamoInexistenteONuloDevuelveVacio() {
        TipoPrestamoJpaRepository repo = mock(TipoPrestamoJpaRepository.class);
        when(repo.findById(99)).thenReturn(Optional.empty());
        TipoPrestamoRepositoryAdapter adapter = new TipoPrestamoRepositoryAdapter(repo);

        assertTrue(adapter.findById(99L).isEmpty());
        assertTrue(adapter.findById(null).isEmpty());
        assertTrue(adapter.findById(Long.MAX_VALUE).isEmpty(), "Un id gigante no debe romper la conversión a entero");
    }

    @Test
    void buscaEstadoPorNombre() {
        EstadoJpaRepository repo = mock(EstadoJpaRepository.class);
        when(repo.findByNombre(Estado.PENDIENTE_REVISION)).thenReturn(Optional.of(
                EstadoEntity.builder().idEstado(1).nombre(Estado.PENDIENTE_REVISION).descripcion("Espera evaluación").build()));
        EstadoRepositoryAdapter adapter = new EstadoRepositoryAdapter(repo);

        Estado estado = adapter.findByNombre(Estado.PENDIENTE_REVISION).orElseThrow();

        assertEquals(1L, estado.getId());
        assertEquals("Espera evaluación", estado.getDescripcion());
    }

    private static SolicitudEntity entidadCompleta(int id, String documento, String estado) {
        return SolicitudEntity.builder()
                .idSolicitud(id).documentoIdentidad(documento).monto(new BigDecimal("1000000")).plazo(12)
                .idTipoPrestamo(2).idEstado(1).fechaCreacion(LocalDateTime.of(2026, 11, 6, 10, 0))
                .tipoPrestamo(TipoPrestamoEntity.builder().idTipoPrestamo(2).nombre("Vehículo")
                        .tasaInteres(new BigDecimal("1.30")).build())
                .estado(EstadoEntity.builder().idEstado(1).nombre(estado).build())
                .build();
    }

    @Test
    void listaSolicitudesPorEstadosPaginadasYOrdenadasPorAntiguedad() {
        SolicitudJpaRepository repo = mock(SolicitudJpaRepository.class);
        SolicitudRepositoryAdapter adapter = new SolicitudRepositoryAdapter(repo, mock(ObjectMapper.class));
        List<String> estados = List.of(Estado.PENDIENTE_REVISION, Estado.RECHAZADA);
        when(repo.buscarPorEstados(eq(estados), eq(2), eq("3000000000"), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(entidadCompleta(5, "3000000000", Estado.PENDIENTE_REVISION)),
                        inv.getArgument(3), 11));

        Pagina<Solicitud> pagina = adapter.findByEstados(estados,
                new FiltroSolicitudes(null, 2L, "3000000000"), 1, 5);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repo).buscarPorEstados(eq(estados), eq(2), eq("3000000000"), captor.capture());
        assertEquals(1, captor.getValue().getPageNumber());
        assertEquals(5, captor.getValue().getPageSize());
        assertEquals(Sort.Direction.ASC, captor.getValue().getSort().getOrderFor("fechaCreacion").getDirection());

        assertEquals(11, pagina.totalElementos());
        assertEquals(3, pagina.totalPaginas());
        Solicitud s = pagina.contenido().getFirst();
        assertEquals(5L, s.getId());
        assertEquals("Vehículo", s.getTipoPrestamo().getNombre());
        assertEquals(new BigDecimal("1.30"), s.getTipoPrestamo().getTasaInteres());
        assertEquals(Estado.PENDIENTE_REVISION, s.getEstado().getNombre());
    }

    @Test
    void listaSolicitudesSinFiltroDeTipo() {
        SolicitudJpaRepository repo = mock(SolicitudJpaRepository.class);
        SolicitudRepositoryAdapter adapter = new SolicitudRepositoryAdapter(repo, mock(ObjectMapper.class));
        when(repo.buscarPorEstados(any(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Pagina<Solicitud> pagina = adapter.findByEstados(List.of(Estado.RECHAZADA), FiltroSolicitudes.sinFiltros(), 0, 10);

        assertTrue(pagina.contenido().isEmpty());
    }

    @Test
    void buscaLasAprobadasDeVariosClientes() {
        SolicitudJpaRepository repo = mock(SolicitudJpaRepository.class);
        SolicitudRepositoryAdapter adapter = new SolicitudRepositoryAdapter(repo, mock(ObjectMapper.class));
        when(repo.buscarPorDocumentosYEstado(List.of("3000000000"), Estado.APROBADA))
                .thenReturn(List.of(entidadCompleta(9, "3000000000", Estado.APROBADA)));

        List<Solicitud> aprobadas = adapter.findByDocumentosAndEstado(List.of("3000000000"), Estado.APROBADA);

        assertEquals(1, aprobadas.size());
        assertEquals(new BigDecimal("1.30"), aprobadas.getFirst().getTipoPrestamo().getTasaInteres());
        assertEquals(Estado.APROBADA, aprobadas.getFirst().getEstado().getNombre());
    }
}
