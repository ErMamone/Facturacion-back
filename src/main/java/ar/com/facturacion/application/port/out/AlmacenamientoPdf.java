package ar.com.facturacion.application.port.out;

import ar.com.facturacion.domain.model.Factura;
import java.net.URI;
import java.util.Collection;
import java.util.Set;

public interface AlmacenamientoPdf {

    /** @return la clave con la que quedó guardado (se persiste junto a la factura) */
    String guardar(Factura factura, byte[] pdf);

    /** URL firmada y temporal para que el cliente descargue directo, sin pasar por el backend. */
    URI urlDescarga(String clave, String nombreArchivo);

    /**
     * Borrar una clave inexistente no es error.
     *
     * @return las claves que no se pudieron borrar
     */
    Set<String> borrar(Collection<String> claves);
}
