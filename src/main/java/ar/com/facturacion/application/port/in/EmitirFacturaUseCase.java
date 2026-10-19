package ar.com.facturacion.application.port.in;

import ar.com.facturacion.application.model.EmitirFacturaCommand;
import ar.com.facturacion.domain.model.Factura;

public interface EmitirFacturaUseCase {

    /** Emite la factura, genera su PDF y lo deja en el almacenamiento. */
    Factura emitir(EmitirFacturaCommand command);
}
