package ar.com.facturacion.application.service;

import ar.com.facturacion.application.model.FacturaResumen;
import ar.com.facturacion.application.model.FiltroFacturas;
import ar.com.facturacion.application.model.Pagina;
import ar.com.facturacion.application.model.PaginaSolicitada;
import ar.com.facturacion.application.port.in.BuscarFacturasUseCase;
import ar.com.facturacion.application.port.in.ConsultarFacturaUseCase;
import ar.com.facturacion.application.port.in.DescargarPdfUseCase;
import ar.com.facturacion.application.port.out.AlmacenamientoPdf;
import ar.com.facturacion.application.port.out.FacturaRepository;
import ar.com.facturacion.domain.exception.FacturaNoEncontradaException;
import ar.com.facturacion.domain.model.Factura;
import java.net.URI;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/**
 * Casos de uso de lectura. Todas las consultas filtran por vigencia, así lo vencido no se ve
 * aunque la purga todavía no haya corrido.
 */
@RequiredArgsConstructor
public class ConsultarFacturasService implements BuscarFacturasUseCase, ConsultarFacturaUseCase,
        DescargarPdfUseCase {

    private final FacturaRepository facturas;
    private final AlmacenamientoPdf almacenamiento;
    private final Clock clock;

    @Override
    public Pagina<FacturaResumen> buscar(FiltroFacturas filtro, PaginaSolicitada pagina) {
        return facturas.buscarVigentes(filtro, pagina, clock.instant());
    }

    @Override
    public Factura consultar(UUID id) {
        return facturas.buscarVigente(id, clock.instant())
                .orElseThrow(() -> new FacturaNoEncontradaException(id));
    }

    @Override
    public URI urlDescarga(UUID facturaId) {
        return facturas.buscarClavePdfVigente(facturaId, clock.instant())
                .map(clave -> almacenamiento.urlDescarga(clave, "factura-" + facturaId + ".pdf"))
                .orElseThrow(() -> new FacturaNoEncontradaException(facturaId));
    }
}
