package ar.com.facturacion.infrastructure.web.dto;

import ar.com.facturacion.domain.model.Moneda;
import ar.com.facturacion.infrastructure.web.validation.CuitValido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Cuerpo de {@code POST /api/facturas}. Si el cliente manda {@code total} se ignora: lo calcula el backend.
 * Las reglas se repiten en el dominio (que es la garantía); acá están para devolver errores por campo.
 */
public record EmitirFacturaRequest(
        @NotNull @Valid EmpresaRequest vendedor,
        @NotNull @Valid EmpresaRequest comprador,
        @NotNull Moneda moneda,
        @NotNull @Size(min = 1, max = 50) List<@NotNull @Valid ItemRequest> items) {

    @AssertTrue(message = "vendedor y comprador deben tener CUIT distinto")
    public boolean isPartesDistintas() {
        if (vendedor == null || comprador == null || vendedor.cuit() == null) {
            return true;
        }
        return !vendedor.cuit().equals(comprador.cuit());
    }

    public record EmpresaRequest(
            @NotBlank @CuitValido String cuit,
            @NotBlank @Size(max = 150) String nombre) {
    }

    public record ItemRequest(
            @NotBlank @Size(max = 200) String nombre,
            @NotNull @Positive Integer cantidad,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 13, fraction = 2)
            BigDecimal precio) {
    }
}
