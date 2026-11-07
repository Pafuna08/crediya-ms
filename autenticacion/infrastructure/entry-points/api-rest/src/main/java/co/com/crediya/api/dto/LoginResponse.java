package co.com.crediya.api.dto;

import co.com.crediya.model.seguridad.Token;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Respuesta del inicio de sesión. El token se envía en las siguientes peticiones
 * en la cabecera: Authorization: Bearer &lt;token&gt;
 */
public record LoginResponse(
        String token,
        @JsonProperty("tipo_token") String tipoToken,
        @JsonProperty("expira_en_segundos") long expiraEnSegundos
) {

    public static LoginResponse fromDomain(Token token) {
        return new LoginResponse(token.valor(), "Bearer", token.expiraEnSegundos());
    }
}
