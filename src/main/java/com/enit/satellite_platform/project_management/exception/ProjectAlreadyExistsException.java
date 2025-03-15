package com.enit.satellite_platform.project_management.exception;

public class ProjectAlreadyExistsException extends RuntimeException{
    public ProjectAlreadyExistsException(String message) {
        super(message);
    }

}
