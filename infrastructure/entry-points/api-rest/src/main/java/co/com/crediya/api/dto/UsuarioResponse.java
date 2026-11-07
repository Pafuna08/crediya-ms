package co.com.crediya.api.dto;

import co.com.crediya.model.user.User;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Respuesta JSON con los datos del usuario registrado.
 */
public record UsuarioResponse(
        Long id,
        String nombres,
        String apellidos,
        @JsonProperty("fecha_nacimiento") LocalDate fechaNacimiento,
        String direccion,
        String telefono,
        @JsonProperty("correo_electronico") String correoElectronico,
        @JsonProperty("salario_base") BigDecimal salarioBase
) {

    public static UsuarioResponse fromDomain(User user) {
        return new UsuarioResponse(
                user.getId(),
                user.getNombres(),
                user.getApellidos(),
                user.getFechaNacimiento(),
                user.getDireccion(),
                user.getTelefono(),
                user.getCorreoElectronico(),
                user.getSalarioBase());
    }
}
