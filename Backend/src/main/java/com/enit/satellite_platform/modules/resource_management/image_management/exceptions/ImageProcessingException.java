package com.enit.satellite_platform.modules.resource_management.image_management.exceptions;

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
