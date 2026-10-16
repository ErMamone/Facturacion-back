package ar.com.facturacion.application.model;

import ar.com.facturacion.domain.exception.ReglaDeNegocioException;

/** Página pedida (base 0). */
public record PaginaSolicitada(int numero, int tamanio) {

    public static final int TAMANIO_MAX = 100;

    public PaginaSolicitada {
        if (numero < 0 || tamanio < 1 || tamanio > TAMANIO_MAX) {
            throw new ReglaDeNegocioException("Paginado inválido: page >= 0 y 1 <= size <= " + TAMANIO_MAX);
        }
    }
}
