package com.enit.satellite_platform.resources_management.exception;

/**
 * Base exception class for image-related errors
 */
public class ImageException extends RuntimeException {
    public ImageException(String message) {
        super(message);
    }

    public ImageException(String message, Throwable cause) {
        super(message, cause);
    }
}
