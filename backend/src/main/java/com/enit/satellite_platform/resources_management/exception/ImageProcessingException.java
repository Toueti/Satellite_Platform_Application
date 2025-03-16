package com.enit.satellite_platform.resources_management.exception;

/**
 * Thrown when there are issues processing or manipulating an image
 */
public class ImageProcessingException extends ImageException {
    public ImageProcessingException(String message) {
        super(message);
    }

    public ImageProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
