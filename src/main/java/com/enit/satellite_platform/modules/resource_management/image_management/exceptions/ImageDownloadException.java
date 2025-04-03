package com.enit.satellite_platform.modules.resource_management.image_management.exceptions;

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
