package ar.com.facturacion.application.port.in;

import ar.com.facturacion.application.model.ResultadoPurga;

public interface PurgarFacturasVencidasUseCase {

    /** Borra facturas vencidas (PDF + datos) y las empresas que quedaron sin facturas. Idempotente. */
    ResultadoPurga purgar();
}
