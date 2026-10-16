package ar.com.facturacion.application.port.in;

import ar.com.facturacion.domain.model.Factura;
import java.util.UUID;

public interface ConsultarFacturaUseCase {

    /** @throws ar.com.facturacion.domain.exception.FacturaNoEncontradaException si no existe o venció */
    Factura consultar(UUID id);
}
