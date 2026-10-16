package ar.com.facturacion.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.com.facturacion.application.model.EmitirFacturaCommand;
import ar.com.facturacion.application.port.out.AlmacenamientoPdf;
import ar.com.facturacion.application.port.out.EmpresaRepository;
import ar.com.facturacion.application.port.out.FacturaRepository;
import ar.com.facturacion.application.port.out.GeneradorPdf;
import ar.com.facturacion.application.port.out.UnidadDeTrabajo;
import ar.com.facturacion.domain.model.Cuit;
import ar.com.facturacion.domain.model.Empresa;
import ar.com.facturacion.domain.model.Item;
import ar.com.facturacion.domain.model.Moneda;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmitirFacturaServiceTest {

    static final Instant AHORA = Instant.parse("2026-10-15T19:30:00Z");
    // CUITs elegidos para que el comprador quede antes que el vendedor al ordenar
    static final Empresa VENDEDOR = new Empresa(new Cuit("30712345671"), "Vendedor");
    static final Empresa COMPRADOR = new Empresa(new Cuit("20123456786"), "Comprador");

    @Mock FacturaRepository facturas;
    @Mock EmpresaRepository empresas;
    @Mock GeneradorPdf generadorPdf;
    @Mock AlmacenamientoPdf almacenamiento;

    /** Unidad de trabajo trivial: ejecuta en línea (la transacción real se prueba en los tests de integración). */
    final UnidadDeTrabajo enLinea = new UnidadDeTrabajo() {
        @Override
        public <T> T ejecutar(Supplier<T> trabajo) {
            return trabajo.get();
        }
    };

    EmitirFacturaService service;

    @BeforeEach
    void setUp() {
        service = new EmitirFacturaService(facturas, empresas, generadorPdf, almacenamiento, enLinea,
                Clock.fixed(AHORA, ZoneOffset.UTC), Duration.ofMinutes(10));
        when(generadorPdf.generar(any())).thenReturn(new byte[] {1});
        when(almacenamiento.guardar(any(), any())).thenReturn("facturas/2026/10/x.pdf");
    }

    @Test
    void emiteGuardaEmpresasEnOrdenDeCuitYPersisteConLaClaveDelPdf() {
        var factura = service.emitir(command());

        assertThat(factura.getTotal()).isEqualByComparingTo("25.00");
        assertThat(factura.getVenceEl()).isEqualTo(AHORA.plus(Duration.ofMinutes(10)));
        assertThat(factura.getClavePdf()).isEqualTo("facturas/2026/10/x.pdf");

        var orden = inOrder(almacenamiento, empresas, facturas);
        orden.verify(almacenamiento).guardar(any(), any());
        orden.verify(empresas).guardar(COMPRADOR);
        orden.verify(empresas).guardar(VENDEDOR);
        orden.verify(facturas).guardar(argThat(f -> "facturas/2026/10/x.pdf".equals(f.getClavePdf())));
    }

    @Test
    void siFallaLaPersistenciaBorraElPdfSubido() {
        doThrow(new IllegalStateException("db caída")).when(facturas).guardar(any());

        assertThatThrownBy(() -> service.emitir(command())).hasMessage("db caída");

        verify(almacenamiento).borrar(List.of("facturas/2026/10/x.pdf"));
    }

    private static EmitirFacturaCommand command() {
        return new EmitirFacturaCommand(VENDEDOR, COMPRADOR, Moneda.USD,
                List.of(new Item("Servicio", 2, new BigDecimal("12.50"))));
    }
}
