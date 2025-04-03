package com.enit.satellite_platform.modules.resource_management.image_management.exceptions;

/**
 * Thrown when image data or parameters are invalid
 */
public class InvalidImageDataException extends ImageException {
    public InvalidImageDataException(String message) {
        super(message);
    }

    public InvalidImageDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
