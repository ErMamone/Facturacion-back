package ar.com.facturacion.infrastructure.persistence.adapter;

import ar.com.facturacion.application.port.out.EmpresaRepository;
import ar.com.facturacion.domain.model.Empresa;
import ar.com.facturacion.infrastructure.persistence.repository.SpringDataEmpresaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional
public class EmpresaRepositoryAdapter implements EmpresaRepository {

    private final SpringDataEmpresaRepository empresas;

    @Override
    public void guardar(Empresa empresa) {
        // INSERT ... ON CONFLICT: atómico aunque dos altas con la misma empresa lleguen a la vez
        empresas.upsert(empresa.cuit().valor(), empresa.nombre());
    }

    @Override
    public int borrarSinFacturas() {
        return empresas.borrarSinFacturas();
    }
}
