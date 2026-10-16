package ar.com.facturacion.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.com.facturacion.application.model.FacturaVencida;
import ar.com.facturacion.application.port.out.AlmacenamientoPdf;
import ar.com.facturacion.application.port.out.EmpresaRepository;
import ar.com.facturacion.application.port.out.FacturaRepository;
import ar.com.facturacion.application.port.out.UnidadDeTrabajo;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PurgarFacturasVencidasServiceTest {

    @Mock FacturaRepository facturas;
    @Mock EmpresaRepository empresas;
    @Mock AlmacenamientoPdf almacenamiento;

    final UnidadDeTrabajo enLinea = new UnidadDeTrabajo() {
        @Override
        public <T> T ejecutar(Supplier<T> trabajo) {
            return trabajo.get();
        }
    };

    PurgarFacturasVencidasService service;

    @BeforeEach
    void setUp() {
        service = new PurgarFacturasVencidasService(facturas, empresas, almacenamiento, enLinea,
                Clock.fixed(Instant.parse("2026-10-15T00:00:00Z"), ZoneOffset.UTC), 2);
    }

    @Test
    void borraPorLotesHastaQueNoQuedanVencidas() {
        var a = vencida("a");
        var b = vencida("b");
        var c = vencida("c");
        when(facturas.buscarVencidas(any(), anyInt())).thenReturn(List.of(a, b), List.of(c));
        when(almacenamiento.borrar(any())).thenReturn(Set.of());
        when(facturas.borrar(any())).thenAnswer(inv -> ((List<?>) inv.getArgument(0)).size());
        when(empresas.borrarSinFacturas()).thenReturn(3);

        var resultado = service.purgar();

        assertThat(resultado.facturasBorradas()).isEqualTo(3);
        assertThat(resultado.empresasBorradas()).isEqualTo(3);
        verify(facturas).borrar(List.of(a.id(), b.id()));
        verify(facturas).borrar(List.of(c.id()));
    }

    @Test
    void siUnPdfNoSePudoBorrarLaFilaQuedaParaReintentar() {
        var ok = vencida("ok");
        var falla = vencida("falla");
        when(facturas.buscarVencidas(any(), anyInt())).thenReturn(List.of(ok, falla), List.of());
        when(almacenamiento.borrar(any())).thenReturn(Set.of("falla"));
        when(facturas.borrar(any())).thenReturn(1);

        var resultado = service.purgar();

        verify(facturas).borrar(List.of(ok.id()));
        assertThat(resultado.pdfsConError()).isEqualTo(1);
    }

    @Test
    void siFallanTodosLosPdfsCortaSinBorrarFilas() {
        var falla = vencida("falla");
        var falla2 = vencida("falla2");
        when(facturas.buscarVencidas(any(), anyInt())).thenReturn(List.of(falla, falla2));
        when(almacenamiento.borrar(any())).thenReturn(Set.of("falla", "falla2"));

        var resultado = service.purgar();

        verify(facturas, never()).borrar(any());
        assertThat(resultado.facturasBorradas()).isZero();
        assertThat(resultado.pdfsConError()).isEqualTo(2);
    }

    private static FacturaVencida vencida(String clave) {
        return new FacturaVencida(UUID.randomUUID(), clave);
    }
}
