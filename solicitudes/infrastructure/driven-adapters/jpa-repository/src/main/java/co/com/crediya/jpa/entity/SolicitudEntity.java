package co.com.crediya.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tabla "solicitud". El tipo de préstamo y el estado se guardan como llaves foráneas;
 * sus datos completos los aporta el caso de uso.
 */
@Entity
@Table(name = "solicitud")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Integer idSolicitud;

    @Column(name = "documento_identidad", nullable = false, length = 20)
    private String documentoIdentidad;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(name = "plazo", nullable = false)
    private Integer plazo;

    @Column(name = "id_tipo_prestamo", nullable = false)
    private Integer idTipoPrestamo;

    @Column(name = "id_estado", nullable = false)
    private Integer idEstado;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /** Tipo de préstamo completo, solo de lectura (se escribe con idTipoPrestamo). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_prestamo", insertable = false, updatable = false)
    private TipoPrestamoEntity tipoPrestamo;

    /** Estado completo, solo de lectura (se escribe con idEstado). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado", insertable = false, updatable = false)
    private EstadoEntity estado;

    @PrePersist
    public void asignarFechaCreacion() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
