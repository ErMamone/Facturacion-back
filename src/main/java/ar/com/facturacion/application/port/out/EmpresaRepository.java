package ar.com.facturacion.application.port.out;

import ar.com.facturacion.domain.model.Empresa;

public interface EmpresaRepository {

    /** Alta o actualización por CUIT (último nombre gana). Atómico frente a altas concurrentes. */
    void guardar(Empresa empresa);

    /** @return cuántas empresas sin facturas se borraron */
    int borrarSinFacturas();
}
