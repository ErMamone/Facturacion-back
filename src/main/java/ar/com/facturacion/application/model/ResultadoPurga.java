package ar.com.facturacion.application.model;

public record ResultadoPurga(int facturasBorradas, int empresasBorradas, int pdfsConError) {

    public boolean huboCambios() {
        return facturasBorradas > 0 || empresasBorradas > 0 || pdfsConError > 0;
    }
}
