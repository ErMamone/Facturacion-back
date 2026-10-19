package ar.com.facturacion.infrastructure.web.mapper;

import ar.com.facturacion.application.model.EmitirFacturaCommand;
import ar.com.facturacion.application.model.FacturaResumen;
import ar.com.facturacion.application.model.Pagina;
import ar.com.facturacion.domain.model.Cuit;
import ar.com.facturacion.domain.model.Empresa;
import ar.com.facturacion.domain.model.Factura;
import ar.com.facturacion.domain.model.Item;
import ar.com.facturacion.infrastructure.web.dto.EmitirFacturaRequest;
import ar.com.facturacion.infrastructure.web.dto.FacturaResponses.EmpresaResponse;
import ar.com.facturacion.infrastructure.web.dto.FacturaResponses.FacturaDetalleResponse;
import ar.com.facturacion.infrastructure.web.dto.FacturaResponses.FacturaEmitidaResponse;
import ar.com.facturacion.infrastructure.web.dto.FacturaResponses.FacturaResumenResponse;
import ar.com.facturacion.infrastructure.web.dto.FacturaResponses.ItemResponse;
import ar.com.facturacion.infrastructure.web.dto.FacturaResponses.PaginaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** DTOs HTTP ↔ modelo de aplicación/dominio. */
@Mapper
public interface FacturaWebMapper {

    EmitirFacturaCommand toCommand(EmitirFacturaRequest request);

    Empresa toDomain(EmitirFacturaRequest.EmpresaRequest request);

    Item toDomain(EmitirFacturaRequest.ItemRequest request);

    @Mapping(target = "expiresAt", source = "venceEl")
    FacturaEmitidaResponse toEmitidaResponse(Factura factura);

    @Mapping(target = "createdAt", source = "emitidaEl")
    @Mapping(target = "expiresAt", source = "venceEl")
    FacturaDetalleResponse toDetalleResponse(Factura factura);

    @Mapping(target = "createdAt", source = "emitidaEl")
    @Mapping(target = "expiresAt", source = "venceEl")
    FacturaResumenResponse toResumenResponse(FacturaResumen resumen);

    EmpresaResponse toResponse(Empresa empresa);

    @Mapping(target = "subtotal", expression = "java(item.subtotal())")
    ItemResponse toResponse(Item item);

    default PaginaResponse<FacturaResumenResponse> toPaginaResponse(Pagina<FacturaResumen> pagina) {
        var mapeada = pagina.map(this::toResumenResponse);
        return new PaginaResponse<>(mapeada.contenido(), mapeada.numero(), mapeada.tamanio(),
                mapeada.totalElementos(), mapeada.totalPaginas());
    }

    default Cuit toCuit(String valor) {
        return valor == null ? null : new Cuit(valor);
    }

    default String toValor(Cuit cuit) {
        return cuit == null ? null : cuit.valor();
    }
}
