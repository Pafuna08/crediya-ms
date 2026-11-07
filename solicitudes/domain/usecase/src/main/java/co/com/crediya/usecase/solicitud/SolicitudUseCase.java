package co.com.crediya.usecase.solicitud;

import co.com.crediya.model.estado.Estado;
import co.com.crediya.model.estado.gateways.EstadoRepository;
import co.com.crediya.model.exceptions.DatosInvalidosException;
import co.com.crediya.model.exceptions.TipoPrestamoNoExisteException;
import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.model.solicitud.gateways.SolicitudRepository;
import co.com.crediya.model.tipoprestamo.TipoPrestamo;
import co.com.crediya.model.tipoprestamo.gateways.TipoPrestamoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Caso de uso: registrar una solicitud de préstamo.
 * Aquí viven las reglas de negocio; no sabe nada de HTTP ni de base de datos.
 */
@Log
@RequiredArgsConstructor
public class SolicitudUseCase {

    static final int DOCUMENTO_LONGITUD_MAXIMA = 20;
    private static final Pattern FORMATO_DOCUMENTO = Pattern.compile("^[A-Za-z0-9]+$");

    private final SolicitudRepository solicitudRepository;
    private final TipoPrestamoRepository tipoPrestamoRepository;
    private final EstadoRepository estadoRepository;

    public Solicitud registrarSolicitud(Solicitud solicitud) {
        log.info("Iniciando registro de solicitud de préstamo");

        validarDatos(solicitud);

        Long idTipo = solicitud.getTipoPrestamo().getId();
        TipoPrestamo tipoPrestamo = tipoPrestamoRepository.findById(idTipo)
                .orElseThrow(() -> {
                    log.warning("Registro rechazado: el tipo de préstamo " + idTipo + " no existe");
                    return new TipoPrestamoNoExisteException(idTipo);
                });

        // El estado inicial lo decide el sistema, nunca el cliente
        Estado estadoInicial = estadoRepository.findByNombre(Estado.PENDIENTE_REVISION)
                .orElseThrow(() -> new IllegalStateException(
                        "No está configurado el estado inicial '" + Estado.PENDIENTE_REVISION + "' en la base de datos"));

        Solicitud nueva = solicitud.toBuilder()
                .id(null)
                .documentoIdentidad(solicitud.getDocumentoIdentidad().trim())
                .tipoPrestamo(tipoPrestamo)
                .estado(estadoInicial)
                .fechaCreacion(null)
                .build();

        Solicitud guardada = solicitudRepository.save(nueva);
        log.info("Solicitud registrada con id " + guardada.getId() + " en estado '" + estadoInicial.getNombre() + "'");
        return guardada;
    }

    private void validarDatos(Solicitud solicitud) {
        if (solicitud == null) {
            throw new DatosInvalidosException(List.of("Los datos de la solicitud son obligatorios"));
        }

        List<String> errores = new ArrayList<>();

        String documento = solicitud.getDocumentoIdentidad();
        if (documento == null || documento.isBlank()) {
            errores.add("El campo documento_identidad es obligatorio");
        } else if (documento.trim().length() > DOCUMENTO_LONGITUD_MAXIMA) {
            errores.add("El campo documento_identidad no puede tener más de " + DOCUMENTO_LONGITUD_MAXIMA + " caracteres");
        } else if (!FORMATO_DOCUMENTO.matcher(documento.trim()).matches()) {
            errores.add("El campo documento_identidad solo puede contener letras y números");
        }

        BigDecimal monto = solicitud.getMonto();
        if (monto == null) {
            errores.add("El campo monto es obligatorio");
        } else if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            errores.add("El campo monto debe ser mayor que 0");
        }

        Integer plazo = solicitud.getPlazo();
        if (plazo == null) {
            errores.add("El campo plazo es obligatorio");
        } else if (plazo <= 0) {
            errores.add("El campo plazo debe ser mayor que 0 (en meses)");
        }

        if (solicitud.getTipoPrestamo() == null || solicitud.getTipoPrestamo().getId() == null) {
            errores.add("El campo id_tipo_prestamo es obligatorio");
        }

        if (!errores.isEmpty()) {
            log.warning("Registro rechazado por datos inválidos: " + errores);
            throw new DatosInvalidosException(errores);
        }
    }
}
