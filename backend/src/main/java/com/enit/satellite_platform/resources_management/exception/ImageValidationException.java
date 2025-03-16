package com.enit.satellite_platform.resources_management.exception;

/**
 * Thrown when image validation fails
 */
public class ImageValidationException extends ImageException {
    public ImageValidationException(String message) {
        super(message);
    }

    public ImageValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
