package co.com.crediya.model.solicitud;

import co.com.crediya.model.cliente.Cliente;
import co.com.crediya.model.pagina.Pagina;
import co.com.crediya.model.tipoprestamo.TipoPrestamo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SolicitudTest {

    private static Solicitud prestamo(String monto, int plazo, String tasaMensual) {
        return Solicitud.builder()
                .monto(new BigDecimal(monto))
                .plazo(plazo)
                .tipoPrestamo(TipoPrestamo.builder().tasaInteres(tasaMensual == null ? null : new BigDecimal(tasaMensual)).build())
                .build();
    }

    @Test
    void calculaLaCuotaConAmortizacionFrancesa() {
        // Valor de referencia: 1.000.000 al 1 % mensual a 12 meses → 88.848,79
        assertEquals(new BigDecimal("88848.79"), prestamo("1000000", 12, "1.00").cuotaMensual());
    }

    @Test
    void conTasaCeroLaCuotaEsMontoEntrePlazo() {
        assertEquals(new BigDecimal("100000.00"), prestamo("1200000", 12, "0").cuotaMensual());
    }

    @Test
    void sinDatosSuficientesLaCuotaEsCero() {
        assertEquals(BigDecimal.ZERO, Solicitud.builder().build().cuotaMensual());
        assertEquals(BigDecimal.ZERO, prestamo("1000000", 12, null).cuotaMensual());
        assertEquals(BigDecimal.ZERO, prestamo("1000000", 0, "1.00").cuotaMensual());
    }

    @Test
    void calculaElTotalDePaginas() {
        assertEquals(3, new Pagina<>(List.of(), 0, 10, 21).totalPaginas());
        assertEquals(2, new Pagina<>(List.of(), 0, 10, 20).totalPaginas());
        assertEquals(0, new Pagina<>(List.of(), 0, 10, 0).totalPaginas());
        assertEquals(List.of(2, 4), new Pagina<>(List.of(1, 2), 0, 10, 2).map(n -> n * 2).contenido());
    }

    @Test
    void nombreCompletoUneNombresYApellidos() {
        assertEquals("Ana María Gómez", Cliente.builder().nombres("Ana María").apellidos("Gómez").build().nombreCompleto());
        assertEquals("Ana", Cliente.builder().nombres("Ana").build().nombreCompleto());
    }
}
