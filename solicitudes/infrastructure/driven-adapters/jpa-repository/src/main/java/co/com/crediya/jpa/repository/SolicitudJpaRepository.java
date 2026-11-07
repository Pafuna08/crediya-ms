package co.com.crediya.jpa.repository;

import co.com.crediya.jpa.entity.SolicitudEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.QueryByExampleExecutor;

import java.util.Collection;
import java.util.List;

public interface SolicitudJpaRepository
        extends CrudRepository<SolicitudEntity, Integer>, QueryByExampleExecutor<SolicitudEntity> {

    /**
     * Listado de revisión: trae en la misma consulta el tipo de préstamo y el estado de cada solicitud.
     * Los filtros nulos no se aplican.
     */
    @Query(value = """
            select s from SolicitudEntity s
              join fetch s.tipoPrestamo t
              join fetch s.estado e
            where e.nombre in :estados
              and (:idTipoPrestamo is null or s.idTipoPrestamo = :idTipoPrestamo)
              and (:documento is null or s.documentoIdentidad = :documento)
            """,
            countQuery = """
            select count(s) from SolicitudEntity s
              join s.estado e
            where e.nombre in :estados
              and (:idTipoPrestamo is null or s.idTipoPrestamo = :idTipoPrestamo)
              and (:documento is null or s.documentoIdentidad = :documento)
            """)
    Page<SolicitudEntity> buscarPorEstados(@Param("estados") Collection<String> estados,
                                           @Param("idTipoPrestamo") Integer idTipoPrestamo,
                                           @Param("documento") String documento,
                                           Pageable pageable);

    @Query("""
            select s from SolicitudEntity s
              join fetch s.tipoPrestamo t
              join fetch s.estado e
            where e.nombre = :estado
              and s.documentoIdentidad in :documentos
            """)
    List<SolicitudEntity> buscarPorDocumentosYEstado(@Param("documentos") Collection<String> documentos,
                                                    @Param("estado") String estado);
}
