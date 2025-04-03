package com.enit.satellite_platform.modules.resource_management.config;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to validate that the analysis type is one of the allowed values.
 */
@Constraint(validatedBy = AnalysisTypeValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidServiceType {
    String message() default "Invalid analysis type";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}