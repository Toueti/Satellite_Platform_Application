package com.enit.satellite_platform.modules.resource_management.utils.storage_management;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public interface StorageService {
    /**
     * Stores the given file and returns a unique identifier or URI.
     * 
     * @param file     The file to store.
     * @param metadata Optional metadata for the file.
     * @return A string identifier (e.g., URI, file path, or GridFS ID).
     * @throws IOException If storage fails.
     */
    String store(MultipartFile file, Map<String, Object> metadata) throws IOException;

    /**
     * Retrieves the file content as an InputStream based on its identifier.
     * 
     * @param identifier The unique identifier of the file.
     * @return InputStream of the file content.
     * @throws IOException If retrieval fails.
     */
    InputStream retrieve(String identifier) throws IOException;

    /**
     * Deletes the file associated with the given identifier.
     * 
     * @param identifier The unique identifier of the file to delete.
     * @return true if deletion was successful, false otherwise.
     * @throws IOException If deletion fails.
     */
    boolean delete(String identifier) throws IOException;

    /**
     * Returns the storage type (e.g., "gridfs", "filesystem", "s3").
     * 
     * @return The storage type identifier.
     */
    String getStorageType();
}
