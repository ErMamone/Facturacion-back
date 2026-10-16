package ar.com.facturacion.application.model;

import java.util.UUID;

/** Lo mínimo que necesita la purga de cada factura vencida. */
public record FacturaVencida(UUID id, String clavePdf) {
}
