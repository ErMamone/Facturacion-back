package ar.com.facturacion.application.model;

import ar.com.facturacion.domain.exception.ReglaDeNegocioException;
import ar.com.facturacion.domain.model.Cuit;
import java.util.Optional;

/** Filtro de búsqueda: al menos uno de los dos CUIT es obligatorio. */
public record FiltroFacturas(Optional<Cuit> vendedor, Optional<Cuit> comprador) {

    public FiltroFacturas {
        if (vendedor.isEmpty() && comprador.isEmpty()) {
            throw new ReglaDeNegocioException("Indicá al menos un filtro: cuitVendedor o cuitComprador");
        }
    }
}
