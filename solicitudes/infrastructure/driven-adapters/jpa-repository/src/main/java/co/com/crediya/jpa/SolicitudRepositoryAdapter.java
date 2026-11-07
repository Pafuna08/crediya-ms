package co.com.crediya.jpa;

import co.com.crediya.jpa.entity.SolicitudEntity;
import co.com.crediya.jpa.helper.AdapterOperations;
import co.com.crediya.jpa.repository.SolicitudJpaRepository;
import co.com.crediya.model.estado.Estado;
import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.model.solicitud.gateways.SolicitudRepository;
import co.com.crediya.model.tipoprestamo.TipoPrestamo;
import lombok.extern.slf4j.Slf4j;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
