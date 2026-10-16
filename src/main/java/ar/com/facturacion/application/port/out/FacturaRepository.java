package ar.com.facturacion.application.port.out;

import ar.com.facturacion.application.model.FacturaResumen;
import ar.com.facturacion.application.model.FacturaVencida;
import ar.com.facturacion.application.model.FiltroFacturas;
import ar.com.facturacion.application.model.Pagina;
import ar.com.facturacion.application.model.PaginaSolicitada;
import ar.com.facturacion.domain.model.Factura;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FacturaRepository {

    /** Alta de una factura nueva. Las empresas tienen que existir (ver {@link EmpresaRepository#guardar}). */
    void guardar(Factura factura);

    Optional<Factura> buscarVigente(UUID id, Instant ahora);

    Optional<String> buscarClavePdfVigente(UUID id, Instant ahora);

    Pagina<FacturaResumen> buscarVigentes(FiltroFacturas filtro, PaginaSolicitada pagina, Instant ahora);

    /** Las más viejas primero. */
    List<FacturaVencida> buscarVencidas(Instant ahora, int limite);

    int borrar(Collection<UUID> ids);
}
