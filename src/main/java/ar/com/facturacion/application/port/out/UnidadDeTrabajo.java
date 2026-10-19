package ar.com.facturacion.application.port.out;

import java.util.function.Supplier;

/** Límite transaccional, sin atar la capa de aplicación a Spring. */
public interface UnidadDeTrabajo {

    <T> T ejecutar(Supplier<T> trabajo);

    default void ejecutarSinResultado(Runnable trabajo) {
        ejecutar(() -> {
            trabajo.run();
            return null;
        });
    }
}
