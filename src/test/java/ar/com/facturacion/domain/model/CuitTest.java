package ar.com.facturacion.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.com.facturacion.domain.exception.ReglaDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CuitTest {

    @ParameterizedTest
    @ValueSource(strings = {"20123456786", "30712345671", "27999999994", "30500010912", "20000000001"})
    void aceptaCuitsConDigitoVerificadorCorrecto(String cuit) {
        assertThat(Cuit.esValido(cuit)).isTrue();
        assertThat(new Cuit(cuit).valor()).isEqualTo(cuit);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "20123456787",   // dígito verificador incorrecto
            "30712345670",
            "2012345678",    // 10 dígitos
            "201234567860",  // 12 dígitos
            "20-12345678-6", // con guiones
            "2012345678a",
            "",
            "20010000000"    // resto 10: no existe dígito válido
    })
    void rechazaCuitsInvalidos(String cuit) {
        assertThat(Cuit.esValido(cuit)).isFalse();
        assertThatThrownBy(() -> new Cuit(cuit)).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void formateaConGuiones() {
        assertThat(new Cuit("20123456786").formateado()).isEqualTo("20-12345678-6");
    }
}
