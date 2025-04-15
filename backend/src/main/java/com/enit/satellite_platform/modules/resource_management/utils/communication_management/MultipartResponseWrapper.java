package com.enit.satellite_platform.modules.resource_management.utils.communication_management;

import java.io.InputStream;

import lombok.Builder;
import lombok.Data;

/**
 * A generic wrapper class to hold the results of a multipart response
 * that contains both a deserialized JSON object and an identifier for the saved file part.
 *
 * @param <T> The type of the deserialized JSON metadata object.
 */
@Data
@Builder
public class MultipartResponseWrapper<T> {

    private T metadata; // The deserialized JSON part
    private final InputStream fileContent;
    private String storageIdentifier; // Identifier (e.g., path, URI, ID) returned by StorageManager, can be null
    private String originalFilename; // Optional: Original filename from Content-Disposition, can be null

    public MultipartResponseWrapper(T metadata, InputStream fileContent, String storageIdentifier, String originalFilename) {
        if (fileContent == null && storageIdentifier == null) {
            throw new IllegalArgumentException("Either fileContent or storageIdentifier must be provided");
        }
        this.fileContent = fileContent;
        this.metadata = metadata;
        this.storageIdentifier = storageIdentifier;
        this.originalFilename = originalFilename;
    }


    @Override
    public String toString() {
        return "MultipartResponseWrapper{" +
               "metadata=" + (metadata != null ? metadata.getClass().getSimpleName() : "null") +
               ", storageIdentifier='" + storageIdentifier + '\'' +
               ", originalFilename='" + originalFilename + '\'' +
               '}';
    }
}
