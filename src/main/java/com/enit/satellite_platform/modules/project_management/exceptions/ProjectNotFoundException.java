package com.enit.satellite_platform.modules.project_management.exceptions;

public class ProjectNotFoundException extends RuntimeException {
    public ProjectNotFoundException(String message) {
        super(message);
    }
    public ProjectNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
