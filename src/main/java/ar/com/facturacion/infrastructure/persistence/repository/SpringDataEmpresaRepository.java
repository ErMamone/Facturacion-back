package ar.com.facturacion.infrastructure.persistence.repository;

import ar.com.facturacion.infrastructure.persistence.entity.EmpresaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SpringDataEmpresaRepository extends JpaRepository<EmpresaEntity, String> {

    @Modifying
    @Query(value = """
            INSERT INTO empresa (cuit, nombre) VALUES (:cuit, :nombre)
            ON CONFLICT (cuit) DO UPDATE SET nombre = EXCLUDED.nombre
            """, nativeQuery = true)
    void upsert(String cuit, String nombre);

    @Modifying
    @Query(value = """
            DELETE FROM empresa e
            WHERE NOT EXISTS (
                SELECT 1 FROM factura f WHERE f.cuit_vendedor = e.cuit OR f.cuit_comprador = e.cuit
            )
            """, nativeQuery = true)
    int borrarSinFacturas();
}
