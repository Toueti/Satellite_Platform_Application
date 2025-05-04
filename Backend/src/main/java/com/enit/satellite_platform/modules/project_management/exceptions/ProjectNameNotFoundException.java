package com.enit.satellite_platform.modules.project_management.exceptions;

public class ProjectNameNotFoundException extends RuntimeException {
    public ProjectNameNotFoundException(String message) {
        super(message);
    }
    public ProjectNameNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
