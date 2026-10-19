package ar.com.facturacion.infrastructure.web.dto;

import ar.com.facturacion.domain.model.Moneda;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Respuestas de la API de facturas (contrato público: los nombres de campo no se cambian a la ligera). */
public final class FacturaResponses {

    private FacturaResponses() {
    }

    public record FacturaEmitidaResponse(UUID id, BigDecimal total, Moneda moneda, Instant expiresAt) {
    }

    public record EmpresaResponse(String cuit, String nombre) {
    }

    public record ItemResponse(String nombre, int cantidad, BigDecimal precio, BigDecimal subtotal) {
    }

    public record FacturaResumenResponse(UUID id, EmpresaResponse vendedor, EmpresaResponse comprador,
                                         Moneda moneda, BigDecimal total, Instant createdAt, Instant expiresAt) {
    }

    public record FacturaDetalleResponse(UUID id, EmpresaResponse vendedor, EmpresaResponse comprador,
                                         Moneda moneda, BigDecimal total, Instant createdAt, Instant expiresAt,
                                         List<ItemResponse> items) {
    }

    public record PaginaResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    }
}
