package ar.com.facturacion.application.port.out;

import ar.com.facturacion.domain.model.Factura;

public interface GeneradorPdf {

    byte[] generar(Factura factura);
}
