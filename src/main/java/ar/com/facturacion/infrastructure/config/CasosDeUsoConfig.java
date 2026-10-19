package ar.com.facturacion.infrastructure.config;

import ar.com.facturacion.application.port.out.AlmacenamientoPdf;
import ar.com.facturacion.application.port.out.EmpresaRepository;
import ar.com.facturacion.application.port.out.FacturaRepository;
import ar.com.facturacion.application.port.out.GeneradorPdf;
import ar.com.facturacion.application.port.out.UnidadDeTrabajo;
import ar.com.facturacion.application.service.ConsultarFacturasService;
import ar.com.facturacion.application.service.EmitirFacturaService;
import ar.com.facturacion.application.service.PurgarFacturasVencidasService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition root: la capa de aplicación no conoce Spring, así que sus servicios se instancian acá
 * inyectándoles los adaptadores de cada puerto.
 */
@Configuration(proxyBeanMethods = false)
public class CasosDeUsoConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    EmitirFacturaService emitirFacturaService(FacturaRepository facturas, EmpresaRepository empresas,
                                              GeneradorPdf generadorPdf, AlmacenamientoPdf almacenamiento,
                                              UnidadDeTrabajo unidadDeTrabajo, Clock clock, AppProperties props) {
        return new EmitirFacturaService(facturas, empresas, generadorPdf, almacenamiento, unidadDeTrabajo, clock,
                props.retention());
    }

    @Bean
    ConsultarFacturasService consultarFacturasService(FacturaRepository facturas, AlmacenamientoPdf almacenamiento,
                                                      Clock clock) {
        return new ConsultarFacturasService(facturas, almacenamiento, clock);
    }

    @Bean
    PurgarFacturasVencidasService purgarFacturasVencidasService(FacturaRepository facturas,
                                                                EmpresaRepository empresas,
                                                                AlmacenamientoPdf almacenamiento,
                                                                UnidadDeTrabajo unidadDeTrabajo, Clock clock,
                                                                AppProperties props) {
        return new PurgarFacturasVencidasService(facturas, empresas, almacenamiento, unidadDeTrabajo, clock,
                props.purgeBatchSize());
    }
}
