package ar.com.facturacion.domain.model;

import ar.com.facturacion.domain.exception.ReglaDeNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

/**
 * Agregado raíz. Inmutable: los cambios de estado devuelven una instancia nueva.
 * <p>
 * Para facturas nuevas se usa {@link #emitir}, que calcula el total; el builder queda para reconstruir
 * desde persistencia.
 */
@Getter
public final class Factura {

    public static final int MAX_ITEMS = 50;

    private final UUID id;
    private final Empresa vendedor;
    private final Empresa comprador;
    private final Moneda moneda;
    private final List<Item> items;
    private final BigDecimal total;
    private final Instant emitidaEl;
    private final Instant venceEl;
    /** Referencia al PDF en el almacenamiento; {@code null} hasta que se genera. */
    private final String clavePdf;

    @Builder(toBuilder = true, access = AccessLevel.PUBLIC)
    private Factura(UUID id, Empresa vendedor, Empresa comprador, Moneda moneda, List<Item> items, BigDecimal total,
                    Instant emitidaEl, Instant venceEl, String clavePdf) {
        this.id = Objects.requireNonNull(id, "id");
        this.vendedor = Objects.requireNonNull(vendedor, "vendedor");
        this.comprador = Objects.requireNonNull(comprador, "comprador");
        this.moneda = Objects.requireNonNull(moneda, "moneda");
        this.items = List.copyOf(Objects.requireNonNull(items, "items"));
        this.total = Objects.requireNonNull(total, "total");
        this.emitidaEl = Objects.requireNonNull(emitidaEl, "emitidaEl");
        this.venceEl = Objects.requireNonNull(venceEl, "venceEl");
        this.clavePdf = clavePdf;

        if (vendedor.cuit().equals(comprador.cuit())) {
            throw new ReglaDeNegocioException("Vendedor y comprador deben tener CUIT distinto");
        }
        if (this.items.isEmpty() || this.items.size() > MAX_ITEMS) {
            throw new ReglaDeNegocioException("La factura debe tener entre 1 y " + MAX_ITEMS + " ítems");
        }
        if (!venceEl.isAfter(emitidaEl)) {
            throw new ReglaDeNegocioException("El vencimiento debe ser posterior a la emisión");
        }
    }

    /** Emite una factura nueva: asigna id, calcula el total e impone el vencimiento según la retención. */
    public static Factura emitir(Empresa vendedor, Empresa comprador, Moneda moneda, List<Item> items,
                                 Instant ahora, Duration retencion) {
        Objects.requireNonNull(items, "items");
        return Factura.builder()
                .id(UUID.randomUUID())
                .vendedor(vendedor)
                .comprador(comprador)
                .moneda(moneda)
                .items(items)
                .total(calcularTotal(items))
                .emitidaEl(ahora)
                .venceEl(ahora.plus(retencion))
                .build();
    }

    /** Σ cantidad × precio, redondeado a 2 decimales con HALF_UP sobre el total (no por ítem). */
    public static BigDecimal calcularTotal(List<Item> items) {
        return items.stream()
                .map(Item::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public Factura conPdf(String clave) {
        if (clavePdf != null) {
            throw new IllegalStateException("La factura " + id + " ya tiene PDF");
        }
        return toBuilder().clavePdf(Objects.requireNonNull(clave, "clave")).build();
    }

    public boolean estaVencida(Instant ahora) {
        return !venceEl.isAfter(ahora);
    }
}
