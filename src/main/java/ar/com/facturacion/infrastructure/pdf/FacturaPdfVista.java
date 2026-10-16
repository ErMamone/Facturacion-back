package ar.com.facturacion.infrastructure.pdf;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Contrato de datos de la plantilla {@code templates/pdf/factura.html}. Mientras esta clase no cambie, la
 * plantilla se puede reescribir entera sin tocar Java. Son JavaBeans (getters) a propósito: es lo que mejor
 * entiende el lenguaje de expresiones de Thymeleaf.
 */
@Value
@Builder
public class FacturaPdfVista {

    String id;
    String moneda;
    BigDecimal total;
    ZonedDateTime emitidaEl;
    ZonedDateTime venceEl;
    EmpresaPdfVista vendedor;
    EmpresaPdfVista comprador;
    List<ItemPdfVista> items;

    @Value
    @Builder
    public static class EmpresaPdfVista {
        String cuit;
        /** 20-12345678-6 */
        String cuitFormateado;
        String nombre;
    }

    @Value
    @Builder
    public static class ItemPdfVista {
        String nombre;
        int cantidad;
        BigDecimal precio;
        BigDecimal subtotal;
    }
}
