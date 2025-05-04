package com.enit.satellite_platform.modules.project_management.exceptions;

public class ProjectValidationException extends RuntimeException {
    public ProjectValidationException(String message) {
        super(message);
    }
    public ProjectValidationException(String message, Throwable cause) {
        super(message, cause);
    }

}
