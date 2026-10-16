package ar.com.facturacion.application.model;

import ar.com.facturacion.domain.model.Empresa;
import ar.com.facturacion.domain.model.Item;
import ar.com.facturacion.domain.model.Moneda;
import java.util.List;

/** Datos de entrada para emitir una factura. El total no viaja: lo calcula el dominio. */
public record EmitirFacturaCommand(Empresa vendedor, Empresa comprador, Moneda moneda, List<Item> items) {
}
