package com.enit.satellite_platform.modules.resource_management.utils.storage_management.storageImp;

import com.enit.satellite_platform.modules.resource_management.utils.storage_management.StorageService;
import com.enit.satellite_platform.modules.resource_management.utils.storage_management.TemporaryFileMetadata;
import com.enit.satellite_platform.modules.resource_management.utils.storage_management.TemporaryFileMetadataService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class TemporaryFileSystemStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(TemporaryFileSystemStorageService.class);
    private static final String STORAGE_TYPE = "tmp-filesystem";

    private final Path storageLocation;
    private final TemporaryFileMetadataService metadataService;

    public TemporaryFileSystemStorageService(
            @Value("${storage.temporary.filesystem-path}") String storagePath,
            TemporaryFileMetadataService metadataService) {
        this.storageLocation = Paths.get(storagePath).toAbsolutePath().normalize();
        this.metadataService = metadataService;
        log.info("TemporaryFileSystemStorageService initialized with path: {}", this.storageLocation);
    }

    @PostConstruct
    private void init() {
        try {
            Files.createDirectories(this.storageLocation);
            log.info("Created temporary storage directory: {}", this.storageLocation);
        } catch (IOException e) {
            log.error("Could not initialize temporary storage location: {}", this.storageLocation, e);
            throw new RuntimeException("Could not initialize temporary storage location", e);
        }
    }

    @Override
    public String store(MultipartFile file, Map<String, Object> metadata) throws IOException {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "tempfile");
        String fileExtension = "";
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot > 0) {
            fileExtension = originalFilename.substring(lastDot);
        }
        String fileId = UUID.randomUUID().toString() + fileExtension;
        Path targetLocation = this.storageLocation.resolve(fileId);

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored temporary file: {} at path: {}", fileId, targetLocation);

            // Store metadata in Redis
            Instant now = Instant.now();
            TemporaryFileMetadata fileMetadata = new TemporaryFileMetadata(fileId, now, now);
            metadataService.storeMetadata(fileId, fileMetadata);

            return fileId; // Return the generated file ID as the identifier
        } catch (IOException ex) {
            log.error("Could not store temporary file {} at path {}", fileId, targetLocation, ex);
            throw new IOException("Could not store temporary file " + originalFilename, ex);
        }
    }

    @Override
    public InputStream retrieve(String identifier) throws IOException {
        Path filePath = this.storageLocation.resolve(identifier).normalize();
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            log.warn("Attempted to retrieve non-existent or unreadable temporary file: {}", identifier);
            // Check if metadata exists, maybe it was cleaned up already
            if (!metadataService.getMetadata(identifier).isPresent()) {
                 log.warn("Metadata for temporary file {} also not found.", identifier);
            }
            throw new FileNotFoundException("Temporary file not found: " + identifier);
        }

        // Update last access time in Redis
        metadataService.updateLastAccessTime(identifier);
        log.debug("Retrieved temporary file: {}", identifier);
        return Files.newInputStream(filePath);
    }

    @Override
    public boolean delete(String identifier) throws IOException {
        Path filePath = this.storageLocation.resolve(identifier).normalize();
        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("Deleted temporary file from filesystem: {}", identifier);
                return true;
            } else {
                log.warn("Attempted to delete non-existent temporary file from filesystem: {}", identifier);
            }
        } catch (IOException ex) {
            log.error("Could not delete temporary file from filesystem: {}", identifier, ex);
            // Don't rethrow immediately, still try to delete metadata
        } finally {
            // Always attempt to delete metadata, even if file deletion failed or file didn't exist
            metadataService.deleteMetadata(identifier);
        }
        return false; // Return false if file was not found or couldn't be deleted
    }

    @Override
    public String getStorageType() {
        return STORAGE_TYPE;
    }

    // Helper method for cleanup service
    public Path getStorageLocation() {
        return storageLocation;
    }
}
