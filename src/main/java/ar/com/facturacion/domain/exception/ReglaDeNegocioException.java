package ar.com.facturacion.domain.exception;

/** Se violó un invariante del dominio (CUIT inválido, partes iguales, ítems fuera de rango, etc.). */
public class ReglaDeNegocioException extends DominioException {

    public ReglaDeNegocioException(String mensaje) {
        super(mensaje);
    }
}
