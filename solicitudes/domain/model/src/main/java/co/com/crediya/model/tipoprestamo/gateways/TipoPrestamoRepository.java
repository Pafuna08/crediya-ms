package co.com.crediya.model.tipoprestamo.gateways;

import co.com.crediya.model.tipoprestamo.TipoPrestamo;

import java.util.Optional;

public interface TipoPrestamoRepository {

    Optional<TipoPrestamo> findById(Long id);
}
