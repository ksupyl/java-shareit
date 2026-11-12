package ru.practicum.shareit.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Конец брони позже её начала. Аннотация стоит на классе, а не на поле:
 * аннотация на поле видит только своё поле, а здесь нужно сравнить два.
 */
@Documented
@Constraint(validatedBy = StartBeforeEndValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface StartBeforeEnd {

    String message() default "must be after start";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
