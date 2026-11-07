package co.com.crediya.model.cliente.gateways;

import co.com.crediya.model.cliente.Cliente;

import java.util.List;

/**
 * Puerto de salida: obtiene los datos de los clientes a partir de sus documentos.
 * El dominio no sabe de dónde vienen (otro microservicio, una base de datos, etc.).
 */
public interface ClienteGateway {

    /** Los documentos que no existan simplemente no se devuelven. */
    List<Cliente> findByDocumentos(List<String> documentos);
}
