package ar.com.facturacion.infrastructure.scheduling;

import ar.com.facturacion.application.port.in.PurgarFacturasVencidasUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dispara la purga periódicamente.
 * <p>
 * Con más de una réplica correría en todas a la vez (no rompe nada porque la purga es idempotente, pero
 * hace trabajo de más). Para la demo: 1 réplica. Mejora pendiente: ShedLock.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PurgaFacturasScheduler {

    private final PurgarFacturasVencidasUseCase purgarFacturas;

    @Scheduled(fixedDelayString = "${app.purge-interval}", initialDelayString = "${app.purge-interval}")
    public void ejecutar() {
        try {
            var resultado = purgarFacturas.purgar();
            if (resultado.huboCambios()) {
                log.info("Purga: {} facturas y {} empresas borradas, {} PDFs con error",
                        resultado.facturasBorradas(), resultado.empresasBorradas(), resultado.pdfsConError());
            }
        } catch (RuntimeException e) {
            log.error("Falló la purga de facturas vencidas; se reintenta en la próxima corrida", e);
        }
    }
}
