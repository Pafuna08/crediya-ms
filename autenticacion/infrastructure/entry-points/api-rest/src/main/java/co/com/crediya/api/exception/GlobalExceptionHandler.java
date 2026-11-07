package co.com.crediya.api.exception;

import co.com.crediya.api.dto.ApiErrorResponse;
import co.com.crediya.model.exceptions.BusinessException;
import co.com.crediya.model.exceptions.CorreoYaRegistradoException;
import co.com.crediya.model.exceptions.CredencialesInvalidasException;
import co.com.crediya.model.exceptions.DatosInvalidosException;
import co.com.crediya.model.exceptions.DocumentoYaRegistradoException;
import co.com.crediya.model.exceptions.RolNoExisteException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.time.Instant;
import java.util.List;

/**
 * Captura TODAS las excepciones de la API y las convierte en respuestas controladas.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DatosInvalidosException.class)
    public ResponseEntity<ApiErrorResponse> handleDatosInvalidos(DatosInvalidosException ex, HttpServletRequest request) {
        log.warn("Datos inválidos en {}: {}", request.getRequestURI(), ex.getErrores());
        return construir(HttpStatus.BAD_REQUEST, ex.getErrores(), request);
    }

    @ExceptionHandler({CorreoYaRegistradoException.class, DocumentoYaRegistradoException.class})
    public ResponseEntity<ApiErrorResponse> handleYaRegistrado(BusinessException ex, HttpServletRequest request) {
        log.warn("Conflicto en {}: {}", request.getRequestURI(), ex.getMessage());
        return construir(HttpStatus.CONFLICT, List.of(ex.getMessage()), request);
    }

    @ExceptionHandler(RolNoExisteException.class)
    public ResponseEntity<ApiErrorResponse> handleRolNoExiste(RolNoExisteException ex, HttpServletRequest request) {
        log.warn("Rol inválido en {}: {}", request.getRequestURI(), ex.getMessage());
        return construir(HttpStatus.BAD_REQUEST, List.of(ex.getMessage()), request);
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ApiErrorResponse> handleCredenciales(CredencialesInvalidasException ex,
                                                               HttpServletRequest request) {
        log.warn("Inicio de sesión fallido en {}", request.getRequestURI());
        return construir(HttpStatus.UNAUTHORIZED, List.of(ex.getMessage()), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleJsonInvalido(HttpMessageNotReadableException ex,
                                                               HttpServletRequest request) {
        log.warn("Cuerpo de petición ilegible en {}: {}", request.getRequestURI(), ex.getMessage());
        String mensaje = "El cuerpo de la petición no es un JSON válido o tiene campos con formato incorrecto";
        // Solo cuando el JSON es válido pero un campo trae un tipo incorrecto (ej. texto en salario_base)
        if (ex.getCause() instanceof MismatchedInputException tipoIncorrecto && !tipoIncorrecto.getPath().isEmpty()) {
            String campo = tipoIncorrecto.getPath().getLast().getPropertyName();
            if (campo != null) {
                mensaje = "El campo " + campo + " tiene un formato incorrecto" + pistaDeFormato(campo);
            }
        }
        return construir(HttpStatus.BAD_REQUEST, List.of(mensaje), request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex,
                                                            HttpServletRequest request) {
        log.warn("Content-Type no soportado en {}: {}", request.getRequestURI(), ex.getContentType());
        return construir(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                List.of("El Content-Type debe ser application/json"), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMetodo(HttpRequestMethodNotSupportedException ex,
                                                         HttpServletRequest request) {
        log.warn("Método {} no permitido en {}", ex.getMethod(), request.getRequestURI());
        return construir(HttpStatus.METHOD_NOT_ALLOWED,
                List.of("El método " + ex.getMethod() + " no está permitido en este recurso"), request);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleNoEncontrado(Exception ex, HttpServletRequest request) {
        log.warn("Recurso no encontrado: {}", request.getRequestURI());
        return construir(HttpStatus.NOT_FOUND, List.of("El recurso solicitado no existe"), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleInesperado(Exception ex, HttpServletRequest request) {
        // Se registra el detalle completo en el log, pero al cliente solo le llega un mensaje genérico
        log.error("Error inesperado procesando {}", request.getRequestURI(), ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR,
                List.of("Ocurrió un error inesperado. Intente nuevamente más tarde"), request);
    }

    private static String pistaDeFormato(String campo) {
        return switch (campo) {
            case "salario_base" -> ": debe ser un valor numérico";
            case "fecha_nacimiento" -> ": use el formato yyyy-MM-dd";
            default -> "";
        };
    }

    private ResponseEntity<ApiErrorResponse> construir(HttpStatus status, List<String> mensajes,
                                                       HttpServletRequest request) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), mensajes, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
