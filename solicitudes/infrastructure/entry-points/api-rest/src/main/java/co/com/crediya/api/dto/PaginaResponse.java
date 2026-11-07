package co.com.crediya.api.dto;

import co.com.crediya.model.pagina.Pagina;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.function.Function;

/**
 * Respuesta paginada: los elementos de la página y la información para pedir las demás.
 */
public record PaginaResponse<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        @JsonProperty("total_elementos") long totalElementos,
        @JsonProperty("total_paginas") int totalPaginas
) {

    public static <D, T> PaginaResponse<T> fromDomain(Pagina<D> pagina, Function<D, T> transformacion) {
        return new PaginaResponse<>(
                pagina.contenido().stream().map(transformacion).toList(),
                pagina.numero(),
                pagina.tamano(),
                pagina.totalElementos(),
                pagina.totalPaginas());
    }
}
