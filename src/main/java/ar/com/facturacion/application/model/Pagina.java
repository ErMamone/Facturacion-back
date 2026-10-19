package ar.com.facturacion.application.model;

import java.util.List;
import java.util.function.Function;

/** Página de resultados, independiente de Spring Data. */
public record Pagina<T>(List<T> contenido, int numero, int tamanio, long totalElementos, int totalPaginas) {

    public Pagina {
        contenido = List.copyOf(contenido);
    }

    public <R> Pagina<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapeado = contenido.stream().<R>map(mapper).toList();
        return new Pagina<>(mapeado, numero, tamanio, totalElementos, totalPaginas);
    }
}
