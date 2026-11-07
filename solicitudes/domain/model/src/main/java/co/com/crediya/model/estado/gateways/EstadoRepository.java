package co.com.crediya.model.estado.gateways;

import co.com.crediya.model.estado.Estado;

import java.util.Optional;

public interface EstadoRepository {

    Optional<Estado> findByNombre(String nombre);
}
