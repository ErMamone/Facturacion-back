package ar.com.facturacion.domain.model;

import ar.com.facturacion.domain.exception.ReglaDeNegocioException;
import java.util.Objects;

/** Empresa identificada por CUIT. El nombre vigente es el del último alta que la mencionó. */
public record Empresa(Cuit cuit, String nombre) {

    public static final int NOMBRE_MAX = 150;

    public Empresa {
        Objects.requireNonNull(cuit, "cuit");
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDeNegocioException("El nombre de la empresa es obligatorio");
        }
        nombre = nombre.strip();
        if (nombre.length() > NOMBRE_MAX) {
            throw new ReglaDeNegocioException("El nombre de la empresa supera los " + NOMBRE_MAX + " caracteres");
        }
    }
}
