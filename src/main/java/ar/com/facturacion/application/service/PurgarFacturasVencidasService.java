package ar.com.facturacion.application.service;

import ar.com.facturacion.application.model.FacturaVencida;
import ar.com.facturacion.application.model.ResultadoPurga;
import ar.com.facturacion.application.port.in.PurgarFacturasVencidasUseCase;
import ar.com.facturacion.application.port.out.AlmacenamientoPdf;
import ar.com.facturacion.application.port.out.EmpresaRepository;
import ar.com.facturacion.application.port.out.FacturaRepository;
import ar.com.facturacion.application.port.out.UnidadDeTrabajo;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/**
 * Purga por lotes: primero el PDF, después la fila. Si el PDF no se pudo borrar la fila queda y se
 * reintenta en la próxima corrida (nunca se pierde la referencia a un archivo existente).
 * Al final se borran las empresas sin facturas, para que el borrado sea completo.
 */
@RequiredArgsConstructor
public class PurgarFacturasVencidasService implements PurgarFacturasVencidasUseCase {

    private final FacturaRepository facturas;
    private final EmpresaRepository empresas;
    private final AlmacenamientoPdf almacenamiento;
    private final UnidadDeTrabajo unidadDeTrabajo;
    private final Clock clock;
    private final int tamanioLote;

    @Override
    public ResultadoPurga purgar() {
        int borradas = 0;
        int conError = 0;
        List<FacturaVencida> lote;
        do {
            lote = facturas.buscarVencidas(clock.instant(), tamanioLote);
            if (lote.isEmpty()) {
                break;
            }
            var fallidas = almacenamiento.borrar(lote.stream().map(FacturaVencida::clavePdf).toList());
            List<UUID> ids = lote.stream()
                    .filter(f -> !fallidas.contains(f.clavePdf()))
                    .map(FacturaVencida::id)
                    .toList();
            conError += fallidas.size();
            if (ids.isEmpty()) {
                break; // el almacenamiento está fallando: no tiene sentido seguir en esta corrida
            }
            borradas += unidadDeTrabajo.ejecutar(() -> facturas.borrar(ids));
        } while (lote.size() == tamanioLote);

        int empresasBorradas = unidadDeTrabajo.ejecutar(empresas::borrarSinFacturas);
        return new ResultadoPurga(borradas, empresasBorradas, conError);
    }
}
