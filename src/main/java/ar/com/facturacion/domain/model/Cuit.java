package ar.com.facturacion.domain.model;

import ar.com.facturacion.domain.exception.ReglaDeNegocioException;

/**
 * CUIT argentino: 11 dígitos sin guiones con dígito verificador válido (módulo 11).
 * Si existe una instancia, el CUIT es válido.
 */
public record Cuit(String valor) {

    private static final int[] PESOS = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    public Cuit {
        if (valor == null || !esValido(valor)) {
            throw new ReglaDeNegocioException("CUIT inválido: " + valor);
        }
    }

    public static boolean esValido(String cuit) {
        if (cuit.length() != 11) {
            return false;
        }
        int suma = 0;
        for (int i = 0; i < 11; i++) {
            char c = cuit.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
            if (i < 10) {
                suma += (c - '0') * PESOS[i];
            }
        }
        int verificador = 11 - (suma % 11);
        if (verificador == 11) {
            verificador = 0;
        } else if (verificador == 10) {
            // AFIP no emite CUITs con resto 10 (cambia el prefijo): no existe dígito válido
            return false;
        }
        return verificador == cuit.charAt(10) - '0';
    }

    /** Formato de presentación: {@code 20-12345678-6}. */
    public String formateado() {
        return valor.substring(0, 2) + "-" + valor.substring(2, 10) + "-" + valor.substring(10);
    }

    @Override
    public String toString() {
        return valor;
    }
}
