package ar.com.facturacion.application.port.in;

import java.net.URI;
import java.util.UUID;

public interface DescargarPdfUseCase {

    /**
     * URL temporal de descarga directa del PDF.
     *
     * @throws ar.com.facturacion.domain.exception.FacturaNoEncontradaException si no existe o venció
     */
    URI urlDescarga(UUID facturaId);
}
