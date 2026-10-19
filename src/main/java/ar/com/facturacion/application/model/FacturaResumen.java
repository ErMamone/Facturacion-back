package ar.com.facturacion.application.model;

import ar.com.facturacion.domain.model.Empresa;
import ar.com.facturacion.domain.model.Moneda;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Modelo de lectura para listados: evita cargar los ítems de cada factura. */
public record FacturaResumen(UUID id, Empresa vendedor, Empresa comprador, Moneda moneda, BigDecimal total,
                             Instant emitidaEl, Instant venceEl) {
}
