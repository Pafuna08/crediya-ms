package co.com.crediya.api.dto;

import java.time.Instant;
import java.util.List;

/**
 * Formato único de error que devuelve la API, para que el cliente
 * nunca reciba trazas internas ni mensajes inesperados.
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        List<String> mensajes,
        String path
) {
}
