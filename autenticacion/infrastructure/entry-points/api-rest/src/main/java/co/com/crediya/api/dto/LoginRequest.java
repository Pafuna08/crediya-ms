package co.com.crediya.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Cuerpo JSON que recibe el endpoint POST /api/v1/login.
 */
public record LoginRequest(
        @JsonProperty("correo_electronico") String correoElectronico,
        String clave
) {
}
