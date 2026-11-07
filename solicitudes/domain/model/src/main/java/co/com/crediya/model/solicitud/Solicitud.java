package co.com.crediya.model.solicitud;

import co.com.crediya.model.estado.Estado;
import co.com.crediya.model.tipoprestamo.TipoPrestamo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Solicitud de préstamo enviada por un cliente.
 * No depende de ningún framework: ni Spring, ni JPA, ni Jackson.
 */
@Getter
@AllArgsConstructor
@Builder(toBuilder = true)
public class Solicitud {
    private final Long id;
    private final String documentoIdentidad;
    private final BigDecimal monto;
    /** Plazo en meses. */
    private final Integer plazo;
    private final TipoPrestamo tipoPrestamo;
    private final Estado estado;
    private final LocalDateTime fechaCreacion;

    /**
     * Cuota mensual del préstamo con el sistema de amortización francés (cuota fija):
     * cuota = monto * i * (1 + i)^n / ((1 + i)^n - 1), con i = tasa mensual y n = plazo en meses.
     * Si la tasa es 0, la cuota es simplemente monto / plazo.
     */
    public BigDecimal cuotaMensual() {
        if (monto == null || plazo == null || plazo <= 0 || tipoPrestamo == null || tipoPrestamo.getTasaInteres() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal tasaMensual = tipoPrestamo.getTasaInteres().divide(CIEN, MathContext.DECIMAL64);
        if (tasaMensual.signum() == 0) {
            return monto.divide(BigDecimal.valueOf(plazo), 2, RoundingMode.HALF_UP);
        }
        BigDecimal factor = BigDecimal.ONE.add(tasaMensual).pow(plazo, MathContext.DECIMAL64);
        return monto.multiply(tasaMensual).multiply(factor)
                .divide(factor.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
    }

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
}
