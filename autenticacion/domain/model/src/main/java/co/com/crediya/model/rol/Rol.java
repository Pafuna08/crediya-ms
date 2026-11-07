package co.com.crediya.model.rol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Rol de un usuario. Determina qué acciones puede realizar en el sistema.
 */
@Getter
@AllArgsConstructor
@Builder(toBuilder = true)
public class Rol {

    public static final String ADMIN = "ADMIN";
    public static final String ASESOR = "ASESOR";
    public static final String CLIENTE = "CLIENTE";

    private final Long id;
    private final String nombre;
    private final String descripcion;
}
