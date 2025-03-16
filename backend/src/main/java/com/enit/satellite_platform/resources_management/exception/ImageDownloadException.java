package com.enit.satellite_platform.resources_management.exception;

/**
 * Thrown when there are issues with the image download process
 */
public class ImageDownloadException extends ImageException {
    public ImageDownloadException(String message) {
        super(message);
    }

    public ImageDownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
