package co.com.crediya.autenticacion;

import co.com.crediya.model.cliente.Cliente;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Forma en que el microservicio de autenticación devuelve un usuario (solo los campos que se usan aquí).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record UsuarioAutenticacion(
        @JsonProperty("documento_identidad") String documentoIdentidad,
        String nombres,
        String apellidos,
        @JsonProperty("correo_electronico") String correoElectronico,
        @JsonProperty("salario_base") BigDecimal salarioBase
) {

    Cliente toDomain() {
        return Cliente.builder()
                .documentoIdentidad(documentoIdentidad)
                .nombres(nombres)
                .apellidos(apellidos)
                .correoElectronico(correoElectronico)
                .salarioBase(salarioBase)
                .build();
    }
}
