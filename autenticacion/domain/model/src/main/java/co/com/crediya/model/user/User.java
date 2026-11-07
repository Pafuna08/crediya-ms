package co.com.crediya.model.user;

import co.com.crediya.model.rol.Rol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Modelo de dominio que representa a un usuario (solicitante, asesor o administrador).
 * No depende de ningún framework: ni Spring, ni JPA, ni Jackson.
 */
@Getter
@AllArgsConstructor
@Builder(toBuilder = true)
public class User {
    private final Long id;
    private final String nombres;
    private final String apellidos;
    private final String documentoIdentidad;
    private final LocalDate fechaNacimiento;
    private final String direccion;
    private final String telefono;
    private final String correoElectronico;
    private final BigDecimal salarioBase;
    /** Al registrar llega la clave en texto plano; antes de guardarse se reemplaza por su versión cifrada. */
    private final String clave;
    private final Rol rol;
}
