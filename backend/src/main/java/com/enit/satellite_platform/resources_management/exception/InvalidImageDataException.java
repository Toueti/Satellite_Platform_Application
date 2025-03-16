package com.enit.satellite_platform.resources_management.exception;

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
