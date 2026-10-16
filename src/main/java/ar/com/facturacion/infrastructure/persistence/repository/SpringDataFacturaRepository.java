package ar.com.facturacion.infrastructure.persistence.repository;

import ar.com.facturacion.application.model.FacturaVencida;
import ar.com.facturacion.infrastructure.persistence.entity.FacturaEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SpringDataFacturaRepository extends JpaRepository<FacturaEntity, UUID> {

    @Query(value = """
            select f from FacturaEntity f
            join fetch f.vendedor
            join fetch f.comprador
            where (:cuitVendedor is null or f.vendedor.cuit = :cuitVendedor)
              and (:cuitComprador is null or f.comprador.cuit = :cuitComprador)
              and f.venceEl > :ahora
            """, countQuery = """
            select count(f) from FacturaEntity f
            where (:cuitVendedor is null or f.vendedor.cuit = :cuitVendedor)
              and (:cuitComprador is null or f.comprador.cuit = :cuitComprador)
              and f.venceEl > :ahora
            """)
    Page<FacturaEntity> buscarVigentes(@Nullable String cuitVendedor, @Nullable String cuitComprador, Instant ahora,
                                       Pageable pageable);

    @EntityGraph(attributePaths = {"vendedor", "comprador", "items"})
    Optional<FacturaEntity> findByIdAndVenceElAfter(UUID id, Instant ahora);

    @Query("select f.clavePdf from FacturaEntity f where f.id = :id and f.venceEl > :ahora")
    Optional<String> buscarClavePdfVigente(UUID id, Instant ahora);

    @Query("""
            select new ar.com.facturacion.application.model.FacturaVencida(f.id, f.clavePdf)
            from FacturaEntity f where f.venceEl <= :ahora order by f.venceEl
            """)
    List<FacturaVencida> buscarVencidas(Instant ahora, Pageable lote);

    /** Borrado masivo; los ítems caen por el ON DELETE CASCADE de la FK. */
    @Modifying
    @Query("delete from FacturaEntity f where f.id in :ids")
    int borrarPorIds(Collection<UUID> ids);
}
