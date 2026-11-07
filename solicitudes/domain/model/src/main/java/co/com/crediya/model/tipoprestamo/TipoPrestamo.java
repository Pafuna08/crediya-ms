package co.com.crediya.model.tipoprestamo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Tipo de préstamo que ofrece CrediYa (libre inversión, vehículo, etc.).
 */
@Getter
@AllArgsConstructor
@Builder(toBuilder = true)
public class TipoPrestamo {
    private final Long id;
    private final String nombre;
    /** Tasa de interés mensual en porcentaje. */
    private final BigDecimal tasaInteres;
}
