package ar.com.facturacion.infrastructure.web.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * CUIT de 11 dígitos sin guiones con dígito verificador válido. La regla vive en el dominio
 * ({@link ar.com.facturacion.domain.model.Cuit#esValido}); esto solo la expone a Bean Validation para
 * devolver errores por campo. {@code null} es válido (combinar con {@code @NotBlank}).
 */
@Documented
@Constraint(validatedBy = CuitValidator.class)
@Target({FIELD, PARAMETER, RECORD_COMPONENT, TYPE_USE})
@Retention(RUNTIME)
public @interface CuitValido {

    String message() default "CUIT inválido: deben ser 11 dígitos sin guiones y con dígito verificador correcto";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
