package ar.com.facturacion.application.service;

import ar.com.facturacion.application.model.EmitirFacturaCommand;
import ar.com.facturacion.application.port.in.EmitirFacturaUseCase;
import ar.com.facturacion.application.port.out.AlmacenamientoPdf;
import ar.com.facturacion.application.port.out.EmpresaRepository;
import ar.com.facturacion.application.port.out.FacturaRepository;
import ar.com.facturacion.application.port.out.GeneradorPdf;
import ar.com.facturacion.application.port.out.UnidadDeTrabajo;
import ar.com.facturacion.domain.model.Factura;
import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Emisión: dominio → PDF → almacenamiento → base de datos.
 * <p>
 * El PDF se sube antes de la transacción para no tener una conexión de DB tomada durante la llamada a S3.
 * Si después falla la persistencia se compensa borrando el PDF, así no quedan archivos huérfanos.
 */
@Slf4j
@RequiredArgsConstructor
public class EmitirFacturaService implements EmitirFacturaUseCase {

    private final FacturaRepository facturas;
    private final EmpresaRepository empresas;
    private final GeneradorPdf generadorPdf;
    private final AlmacenamientoPdf almacenamiento;
    private final UnidadDeTrabajo unidadDeTrabajo;
    private final Clock clock;
    private final Duration retencion;

    @Override
    public Factura emitir(EmitirFacturaCommand command) {
        var factura = Factura.emitir(command.vendedor(), command.comprador(), command.moneda(), command.items(),
                clock.instant(), retencion);

        String clave = almacenamiento.guardar(factura, generadorPdf.generar(factura));
        var conPdf = factura.conPdf(clave);

        try {
            unidadDeTrabajo.ejecutarSinResultado(() -> {
                // Orden fijo por CUIT: dos altas cruzadas (A→B y B→A) no se bloquean entre sí
                List.of(conPdf.getVendedor(), conPdf.getComprador()).stream()
                        .sorted(Comparator.comparing(e -> e.cuit().valor()))
                        .forEach(empresas::guardar);
                facturas.guardar(conPdf);
            });
        } catch (RuntimeException e) {
            compensar(clave, e);
            throw e;
        }

        log.info("Factura {} emitida: {} {} ({} ítems)", conPdf.getId(), conPdf.getTotal(), conPdf.getMoneda(),
                conPdf.getItems().size());
        return conPdf;
    }

    private void compensar(String clave, RuntimeException causa) {
        try {
            almacenamiento.borrar(List.of(clave));
        } catch (RuntimeException e) {
            causa.addSuppressed(e);
            log.warn("No se pudo borrar el PDF huérfano {}", clave, e);
        }
    }
}
