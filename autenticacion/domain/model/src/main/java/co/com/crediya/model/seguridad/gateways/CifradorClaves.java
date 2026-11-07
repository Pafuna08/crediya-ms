package co.com.crediya.model.seguridad.gateways;

/**
 * Puerto de salida: cifra claves y verifica si una clave coincide con su versión cifrada.
 * El dominio no sabe qué algoritmo se usa.
 */
public interface CifradorClaves {

    String cifrar(String claveEnTextoPlano);

    boolean coincide(String claveEnTextoPlano, String claveCifrada);
}
