package ar.com.facturacion.infrastructure.persistence.mapper;

import ar.com.facturacion.application.model.FacturaResumen;
import ar.com.facturacion.domain.model.Cuit;
import ar.com.facturacion.domain.model.Empresa;
import ar.com.facturacion.domain.model.Factura;
import ar.com.facturacion.domain.model.Item;
import ar.com.facturacion.infrastructure.persistence.entity.EmpresaEntity;
import ar.com.facturacion.infrastructure.persistence.entity.FacturaEntity;
import ar.com.facturacion.infrastructure.persistence.entity.FacturaItemEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/** Traducción entre el modelo de dominio y las entidades JPA. */
@Mapper
public interface FacturaPersistenceMapper {

    Factura toDomain(FacturaEntity entity);

    FacturaResumen toResumen(FacturaEntity entity);

    Empresa toDomain(EmpresaEntity entity);

    Item toDomain(FacturaItemEntity entity);

    /** Las empresas no se mapean: el adaptador pone referencias a las filas ya existentes. */
    @Mapping(target = "vendedor", ignore = true)
    @Mapping(target = "comprador", ignore = true)
    FacturaEntity toEntity(Factura factura);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "factura", ignore = true)
    FacturaItemEntity toEntity(Item item);

    @AfterMapping
    default void enlazarItems(@MappingTarget FacturaEntity entity) {
        entity.getItems().forEach(item -> item.setFactura(entity));
    }

    default Cuit toCuit(String valor) {
        return valor == null ? null : new Cuit(valor);
    }

    default String toValor(Cuit cuit) {
        return cuit == null ? null : cuit.valor();
    }
}
