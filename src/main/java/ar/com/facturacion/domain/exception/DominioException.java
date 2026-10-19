package ar.com.facturacion.domain.exception;

/** Base de los errores de negocio. No depende de HTTP: la traducción a status la hace el adaptador web. */
public abstract class DominioException extends RuntimeException {

    protected DominioException(String mensaje) {
        super(mensaje);
    }
}
