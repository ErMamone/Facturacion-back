package ar.com.facturacion.domain.model;

import ar.com.facturacion.domain.exception.ReglaDeNegocioException;
import java.math.BigDecimal;

/** Línea de factura. Precio positivo con hasta 2 decimales; cantidad entera positiva. */
public record Item(String nombre, int cantidad, BigDecimal precio) {

    public static final int NOMBRE_MAX = 200;

    public Item {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDeNegocioException("El nombre del ítem es obligatorio");
        }
        nombre = nombre.strip();
        if (nombre.length() > NOMBRE_MAX) {
            throw new ReglaDeNegocioException("El nombre del ítem supera los " + NOMBRE_MAX + " caracteres");
        }
        if (cantidad <= 0) {
            throw new ReglaDeNegocioException("La cantidad debe ser mayor a cero");
        }
        if (precio == null || precio.signum() <= 0) {
            throw new ReglaDeNegocioException("El precio debe ser mayor a cero");
        }
        if (precio.stripTrailingZeros().scale() > 2) {
            throw new ReglaDeNegocioException("El precio admite como máximo 2 decimales");
        }
    }

    /** cantidad × precio, sin redondear: el redondeo se aplica una sola vez sobre el total. */
    public BigDecimal subtotal() {
        return precio.multiply(BigDecimal.valueOf(cantidad));
    }
}
