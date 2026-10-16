package ar.com.facturacion.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.com.facturacion.domain.exception.ReglaDeNegocioException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class FacturaTest {

    static final Empresa VENDEDOR = new Empresa(new Cuit("20123456786"), "Vendedor");
    static final Empresa COMPRADOR = new Empresa(new Cuit("30712345671"), "Comprador");
    static final Instant AHORA = Instant.parse("2026-10-15T19:30:00Z");

    @Test
    void emitirCalculaElTotalYElVencimiento() {
        var factura = emitir(List.of(
                new Item("uno", 3, new BigDecimal("10.10")),
                new Item("dos", 2, new BigDecimal("0.05"))));

        assertThat(factura.getId()).isNotNull();
        assertThat(factura.getTotal()).isEqualByComparingTo("30.40").hasScaleOf(2);
        assertThat(factura.getVenceEl()).isEqualTo(AHORA.plus(Duration.ofDays(30)));
        assertThat(factura.getClavePdf()).isNull();
    }

    @Test
    void elTotalSiempreTieneDosDecimales() {
        // Con precios de hasta 2 decimales y cantidades enteras la suma es exacta; el HALF_UP final
        // solo fija la escala, nunca redondea ítem por ítem
        var factura = emitir(List.of(new Item("a", 7, new BigDecimal("1.5")), new Item("b", 1, new BigDecimal("3"))));
        assertThat(factura.getTotal()).isEqualTo(new BigDecimal("13.50"));
    }

    @Test
    void rechazaVendedorIgualAComprador() {
        assertThatThrownBy(() -> Factura.emitir(VENDEDOR, VENDEDOR, Moneda.ARS, List.of(item()), AHORA,
                Duration.ofDays(1)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("CUIT distinto");
    }

    @Test
    void rechazaSinItemsOMasDeCincuenta() {
        assertThatThrownBy(() -> emitir(List.of())).isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> emitir(Collections.nCopies(Factura.MAX_ITEMS + 1, item())))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThat(emitir(Collections.nCopies(Factura.MAX_ITEMS, item())).getItems()).hasSize(Factura.MAX_ITEMS);
    }

    @Test
    void itemValidaCantidadYPrecio() {
        assertThatThrownBy(() -> new Item("x", 0, BigDecimal.ONE)).isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> new Item("x", 1, BigDecimal.ZERO)).isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> new Item("x", 1, new BigDecimal("1.005")))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> new Item(" ", 1, BigDecimal.ONE)).isInstanceOf(ReglaDeNegocioException.class);
        assertThat(new Item("x", 1, new BigDecimal("1.50")).precio()).isEqualByComparingTo("1.5");
    }

    @Test
    void conPdfDevuelveUnaCopiaYNoSePuedeAsignarDosVeces() {
        var factura = emitir(List.of(item()));
        var conPdf = factura.conPdf("clave");

        assertThat(factura.getClavePdf()).isNull();
        assertThat(conPdf.getClavePdf()).isEqualTo("clave");
        assertThat(conPdf.getId()).isEqualTo(factura.getId());
        assertThatThrownBy(() -> conPdf.conPdf("otra")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void estaVencidaDesdeElInstanteDeVencimiento() {
        var factura = emitir(List.of(item()));
        assertThat(factura.estaVencida(factura.getVenceEl().minusMillis(1))).isFalse();
        assertThat(factura.estaVencida(factura.getVenceEl())).isTrue();
    }

    private static Factura emitir(List<Item> items) {
        return Factura.emitir(VENDEDOR, COMPRADOR, Moneda.ARS, items, AHORA, Duration.ofDays(30));
    }

    private static Item item() {
        return new Item("x", 1, BigDecimal.ONE);
    }

}
