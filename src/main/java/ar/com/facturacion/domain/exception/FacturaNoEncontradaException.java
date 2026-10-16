package ar.com.facturacion.domain.exception;

import java.util.UUID;

/** La factura no existe o ya venció: para el cliente es lo mismo. */
public class FacturaNoEncontradaException extends DominioException {

    public FacturaNoEncontradaException(UUID id) {
        super("No existe la factura " + id + " o ya venció");
    }
}
