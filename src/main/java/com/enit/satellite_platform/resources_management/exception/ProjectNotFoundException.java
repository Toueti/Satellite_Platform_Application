package com.enit.satellite_platform.resources_management.exception;

public class ProjectNotFoundException extends ImageException {
    public ProjectNotFoundException(String message) {
        super(message);
    }

    public ProjectNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
