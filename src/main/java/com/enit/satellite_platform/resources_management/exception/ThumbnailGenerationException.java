package com.enit.satellite_platform.resources_management.exception;

/**
 * Thrown when there are issues with the image thumbnail generation
 */
public class ThumbnailGenerationException extends ImageException {
    public ThumbnailGenerationException(String message) {
        super(message);
    }

    public ThumbnailGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
