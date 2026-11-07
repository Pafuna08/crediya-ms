package co.com.crediya.model.pagina;

import java.util.List;
import java.util.function.Function;

/**
 * Una página de resultados.
 *
 * @param contenido      elementos de esta página
 * @param numero         número de página (empieza en 0)
 * @param tamano         cantidad máxima de elementos por página
 * @param totalElementos total de elementos que cumplen el filtro, en todas las páginas
 */
public record Pagina<T>(List<T> contenido, int numero, int tamano, long totalElementos) {

    public Pagina {
        contenido = List.copyOf(contenido);
    }

    public int totalPaginas() {
        return tamano == 0 ? 0 : (int) Math.ceil((double) totalElementos / tamano);
    }

    public <R> Pagina<R> map(Function<T, R> transformacion) {
        return new Pagina<>(contenido.stream().map(transformacion).toList(), numero, tamano, totalElementos);
    }
}
