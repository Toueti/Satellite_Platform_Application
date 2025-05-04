package com.enit.satellite_platform.modules.project_management.exceptions;

public class ProjectAlreadyExistsException extends RuntimeException{
    public ProjectAlreadyExistsException(String message) {
        super(message);
    }
    public ProjectAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

}
