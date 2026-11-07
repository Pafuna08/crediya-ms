package co.com.crediya.api.dto;

import co.com.crediya.model.rol.Rol;
import co.com.crediya.model.user.User;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Cuerpo JSON que recibe el endpoint POST /api/v1/usuarios.
 */
public record RegistrarUsuarioRequest(
        String nombres,
        String apellidos,
        @JsonProperty("documento_identidad") String documentoIdentidad,
        @JsonProperty("fecha_nacimiento") LocalDate fechaNacimiento,
        String direccion,
        String telefono,
        @JsonProperty("correo_electronico") String correoElectronico,
        @JsonProperty("salario_base") BigDecimal salarioBase,
        String clave,
        @JsonProperty("id_rol") Long idRol
) {

    public User toDomain() {
        return User.builder()
                .nombres(nombres)
                .apellidos(apellidos)
                .documentoIdentidad(documentoIdentidad)
                .fechaNacimiento(fechaNacimiento)
                .direccion(direccion)
                .telefono(telefono)
                .correoElectronico(correoElectronico)
                .salarioBase(salarioBase)
                .clave(clave)
                .rol(idRol == null ? null : Rol.builder().id(idRol).build())
                .build();
    }

    /** Evita que la clave aparezca si este objeto se imprime en un log. */
    @Override
    public String toString() {
        return "RegistrarUsuarioRequest[correoElectronico=" + correoElectronico + ", idRol=" + idRol + "]";
    }
}
