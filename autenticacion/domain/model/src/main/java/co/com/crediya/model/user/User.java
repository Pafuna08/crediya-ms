package co.com.crediya.model.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Modelo de dominio que representa a un usuario (solicitante) del sistema.
 * No depende de ningún framework: ni Spring, ni JPA, ni Jackson.
 */
@Getter
@AllArgsConstructor
@Builder(toBuilder = true)
public class User {
    private final Long id;
    private final String nombres;
    private final String apellidos;
    private final LocalDate fechaNacimiento;
    private final String direccion;
    private final String telefono;
    private final String correoElectronico;
    private final BigDecimal salarioBase;
}
