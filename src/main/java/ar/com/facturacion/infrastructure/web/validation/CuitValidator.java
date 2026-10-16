package ar.com.facturacion.infrastructure.web.validation;

import ar.com.facturacion.domain.model.Cuit;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CuitValidator implements ConstraintValidator<CuitValido, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || Cuit.esValido(value);
    }
}
