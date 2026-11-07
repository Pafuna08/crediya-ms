package co.com.crediya.model.seguridad.gateways;

import co.com.crediya.model.seguridad.Token;
import co.com.crediya.model.user.User;

/**
 * Puerto de salida: emite el token de sesión de un usuario autenticado.
 * El dominio no sabe qué formato tiene (JWT u otro).
 */
public interface GeneradorToken {

    Token generar(User usuario);
}
