package ar.com.facturacion.application.port.in;

import ar.com.facturacion.application.model.FacturaResumen;
import ar.com.facturacion.application.model.FiltroFacturas;
import ar.com.facturacion.application.model.Pagina;
import ar.com.facturacion.application.model.PaginaSolicitada;

public interface BuscarFacturasUseCase {

    /** Solo facturas vigentes, de la más nueva a la más vieja. */
    Pagina<FacturaResumen> buscar(FiltroFacturas filtro, PaginaSolicitada pagina);
}
