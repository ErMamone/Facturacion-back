package ar.com.facturacion.infrastructure.transaction;

import ar.com.facturacion.application.port.out.UnidadDeTrabajo;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@RequiredArgsConstructor
public class SpringUnidadDeTrabajo implements UnidadDeTrabajo {

    private final TransactionTemplate transactionTemplate;

    @Override
    public <T> T ejecutar(Supplier<T> trabajo) {
        return transactionTemplate.execute(status -> trabajo.get());
    }
}
