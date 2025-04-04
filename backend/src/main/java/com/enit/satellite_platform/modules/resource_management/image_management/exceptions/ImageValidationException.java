package com.enit.satellite_platform.modules.resource_management.image_management.exceptions;

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
