package co.com.crediya.jpa;

import co.com.crediya.jpa.entity.EstadoEntity;
import co.com.crediya.jpa.entity.SolicitudEntity;
import co.com.crediya.jpa.entity.TipoPrestamoEntity;
import co.com.crediya.jpa.helper.AdapterOperations;
import co.com.crediya.jpa.repository.SolicitudJpaRepository;
import co.com.crediya.model.estado.Estado;
import co.com.crediya.model.pagina.Pagina;
import co.com.crediya.model.solicitud.FiltroSolicitudes;
import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.model.solicitud.gateways.SolicitudRepository;
import co.com.crediya.model.tipoprestamo.TipoPrestamo;
import lombok.extern.slf4j.Slf4j;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Adaptador de salida: implementa el puerto SolicitudRepository usando JPA.
 */
@Slf4j
@Repository
public class SolicitudRepositoryAdapter
        extends AdapterOperations<Solicitud, SolicitudEntity, Integer, SolicitudJpaRepository>
        implements SolicitudRepository {

    public SolicitudRepositoryAdapter(SolicitudJpaRepository repository, ObjectMapper mapper) {
        super(repository, mapper, SolicitudRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional
    public Solicitud save(Solicitud solicitud) {
        log.debug("Guardando solicitud del documento {} en la base de datos", solicitud.getDocumentoIdentidad());
        Solicitud guardada = super.save(solicitud);
        log.info("Solicitud persistida con id {}", guardada.getId());
        // La tabla solo guarda los ids; se conservan los datos completos que ya trae el dominio
        return guardada.toBuilder()
                .tipoPrestamo(solicitud.getTipoPrestamo())
                .estado(solicitud.getEstado())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<Solicitud> findByEstados(List<String> estados, FiltroSolicitudes filtro, int pagina, int tamano) {
        // Primero las más antiguas: llevan más tiempo esperando una decisión
        PageRequest pageRequest = PageRequest.of(pagina, tamano,
                Sort.by(Sort.Order.asc("fechaCreacion"), Sort.Order.asc("idSolicitud")));
        Page<SolicitudEntity> resultado = repository.buscarPorEstados(
                estados, aInteger(filtro.idTipoPrestamo()), filtro.documentoIdentidad(), pageRequest);
        log.debug("Consulta de revisión: {} de {} solicitudes", resultado.getNumberOfElements(), resultado.getTotalElements());
        return new Pagina<>(resultado.getContent().stream().map(SolicitudRepositoryAdapter::toDomainCompleto).toList(),
                pagina, tamano, resultado.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Solicitud> findByDocumentosAndEstado(List<String> documentos, String estado) {
        return repository.buscarPorDocumentosYEstado(documentos, estado).stream()
                .map(SolicitudRepositoryAdapter::toDomainCompleto)
                .toList();
    }

    /** Para resultados de consultas que ya traen el tipo de préstamo y el estado. */
    private static Solicitud toDomainCompleto(SolicitudEntity entity) {
        TipoPrestamoEntity tipo = entity.getTipoPrestamo();
        EstadoEntity estado = entity.getEstado();
        return toDomain(entity).toBuilder()
                .tipoPrestamo(TipoPrestamo.builder()
                        .id(aLong(tipo.getIdTipoPrestamo()))
                        .nombre(tipo.getNombre())
                        .tasaInteres(tipo.getTasaInteres())
                        .build())
                .estado(Estado.builder()
                        .id(aLong(estado.getIdEstado()))
                        .nombre(estado.getNombre())
                        .descripcion(estado.getDescripcion())
                        .build())
                .build();
    }

    @Override
    protected SolicitudEntity toData(Solicitud solicitud) {
        return SolicitudEntity.builder()
                .idSolicitud(aInteger(solicitud.getId()))
                .documentoIdentidad(solicitud.getDocumentoIdentidad())
                .monto(solicitud.getMonto())
                .plazo(solicitud.getPlazo())
                .idTipoPrestamo(aInteger(solicitud.getTipoPrestamo().getId()))
                .idEstado(aInteger(solicitud.getEstado().getId()))
                .fechaCreacion(solicitud.getFechaCreacion())
                .build();
    }

    private static Solicitud toDomain(SolicitudEntity entity) {
        return Solicitud.builder()
                .id(aLong(entity.getIdSolicitud()))
                .documentoIdentidad(entity.getDocumentoIdentidad())
                .monto(entity.getMonto())
                .plazo(entity.getPlazo())
                .tipoPrestamo(TipoPrestamo.builder().id(aLong(entity.getIdTipoPrestamo())).build())
                .estado(Estado.builder().id(aLong(entity.getIdEstado())).build())
                .fechaCreacion(entity.getFechaCreacion())
                .build();
    }

    static Integer aInteger(Long valor) {
        return valor == null ? null : Math.toIntExact(valor);
    }

    static Long aLong(Integer valor) {
        return valor == null ? null : valor.longValue();
    }
}
