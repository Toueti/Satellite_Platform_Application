package com.enit.satellite_platform.resources_management.config;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Validator for the @ValidAnalysisType annotation.
 * Ensures the analysis type is in the list of valid types.
 */
@Component
public class AnalysisTypeValidator implements ConstraintValidator<ValidServiceType, String> {

    private final List<String> validTypes;

    public AnalysisTypeValidator(@Value("${analysis.validTypes}") String validTypes) {
        this.validTypes = Arrays.asList(validTypes.split(","));
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && validTypes.contains(value.toLowerCase());
    }
}
