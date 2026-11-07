package co.com.crediya.model.cliente;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Datos del cliente que solicita un préstamo. La fuente de verdad es el microservicio de autenticación;
 * aquí solo se usan para mostrarlos en el listado de revisión.
 */
@Getter
@AllArgsConstructor
@Builder(toBuilder = true)
public class Cliente {
    private final String documentoIdentidad;
    private final String nombres;
    private final String apellidos;
    private final String correoElectronico;
    private final BigDecimal salarioBase;

    public String nombreCompleto() {
        String nombre = nombres == null ? "" : nombres.trim();
        String apellido = apellidos == null ? "" : apellidos.trim();
        return (nombre + " " + apellido).trim();
    }
}
