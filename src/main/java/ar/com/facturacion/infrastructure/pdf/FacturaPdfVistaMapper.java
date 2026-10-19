package ar.com.facturacion.infrastructure.pdf;

import ar.com.facturacion.domain.model.Empresa;
import ar.com.facturacion.domain.model.Factura;
import ar.com.facturacion.domain.model.Item;
import ar.com.facturacion.infrastructure.pdf.FacturaPdfVista.EmpresaPdfVista;
import ar.com.facturacion.infrastructure.pdf.FacturaPdfVista.ItemPdfVista;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface FacturaPdfVistaMapper {

    FacturaPdfVista toVista(Factura factura, @Context ZoneId zona);

    @Mapping(target = "cuit", expression = "java(empresa.cuit().valor())")
    @Mapping(target = "cuitFormateado", expression = "java(empresa.cuit().formateado())")
    EmpresaPdfVista toVista(Empresa empresa);

    @Mapping(target = "subtotal", expression = "java(item.subtotal())")
    ItemPdfVista toVista(Item item);

    default ZonedDateTime enZona(Instant instante, @Context ZoneId zona) {
        return instante.atZone(zona);
    }
}
