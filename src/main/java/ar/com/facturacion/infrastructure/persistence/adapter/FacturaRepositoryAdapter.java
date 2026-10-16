package ar.com.facturacion.infrastructure.persistence.adapter;

import ar.com.facturacion.application.model.FacturaResumen;
import ar.com.facturacion.application.model.FacturaVencida;
import ar.com.facturacion.application.model.FiltroFacturas;
import ar.com.facturacion.application.model.Pagina;
import ar.com.facturacion.application.model.PaginaSolicitada;
import ar.com.facturacion.application.port.out.FacturaRepository;
import ar.com.facturacion.domain.model.Cuit;
import ar.com.facturacion.domain.model.Factura;
import ar.com.facturacion.infrastructure.persistence.mapper.FacturaPersistenceMapper;
import ar.com.facturacion.infrastructure.persistence.repository.SpringDataEmpresaRepository;
import ar.com.facturacion.infrastructure.persistence.repository.SpringDataFacturaRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacturaRepositoryAdapter implements FacturaRepository {

    private static final Sort MAS_NUEVAS_PRIMERO = Sort.by(Sort.Direction.DESC, "emitidaEl");

    private final SpringDataFacturaRepository facturas;
    private final SpringDataEmpresaRepository empresas;
    private final FacturaPersistenceMapper mapper;

    @Override
    @Transactional
    public void guardar(Factura factura) {
        var entity = mapper.toEntity(factura);
        entity.setVendedor(empresas.getReferenceById(factura.getVendedor().cuit().valor()));
        entity.setComprador(empresas.getReferenceById(factura.getComprador().cuit().valor()));
        facturas.save(entity);
    }

    @Override
    public Optional<Factura> buscarVigente(UUID id, Instant ahora) {
        return facturas.findByIdAndVenceElAfter(id, ahora).map(mapper::toDomain);
    }

    @Override
    public Optional<String> buscarClavePdfVigente(UUID id, Instant ahora) {
        return facturas.buscarClavePdfVigente(id, ahora);
    }

    @Override
    public Pagina<FacturaResumen> buscarVigentes(FiltroFacturas filtro, PaginaSolicitada pagina, Instant ahora) {
        var page = facturas.buscarVigentes(
                filtro.vendedor().map(Cuit::valor).orElse(null),
                filtro.comprador().map(Cuit::valor).orElse(null),
                ahora,
                PageRequest.of(pagina.numero(), pagina.tamanio(), MAS_NUEVAS_PRIMERO));
        return new Pagina<>(page.getContent().stream().map(mapper::toResumen).toList(), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Override
    public List<FacturaVencida> buscarVencidas(Instant ahora, int limite) {
        return facturas.buscarVencidas(ahora, PageRequest.ofSize(limite));
    }

    @Override
    @Transactional
    public int borrar(Collection<UUID> ids) {
        return ids.isEmpty() ? 0 : facturas.borrarPorIds(ids);
    }
}
