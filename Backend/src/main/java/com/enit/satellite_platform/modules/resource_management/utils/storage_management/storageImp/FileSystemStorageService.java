package com.enit.satellite_platform.modules.resource_management.utils.storage_management.storageImp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.enit.satellite_platform.modules.resource_management.utils.storage_management.StorageManager;
import com.enit.satellite_platform.modules.resource_management.utils.storage_management.StorageService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Service("fileSystemStorageService")
public class FileSystemStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageManager.class);

    @Value("${storage.filesystem.directory:/tmp/images}")
    private String storageDirectory;


    @Override
    public String store(MultipartFile file, Map<String, Object> metadata) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file");
        }

        // Create a clean filename and ensure file extension is preserved
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFileName = UUID.randomUUID().toString() + (fileExtension.isEmpty() ? "" : "." + fileExtension);

        // Create target path
        Path targetDirectory = Paths.get(storageDirectory).toAbsolutePath().normalize();
        Path targetPath = targetDirectory.resolve(uniqueFileName);

        // Create directories if they don't exist
        Files.createDirectories(targetDirectory);

        // Check if target location is valid (outside of our storage directory)
        if (!targetPath.getParent().equals(targetDirectory)) {
            throw new IOException("Cannot store file outside of designated directory");
        }

        // Store the file using transferTo for more efficient handling of large files
        log.info("Storing file: {} as {}", originalFilename, uniqueFileName);
        file.transferTo(targetPath);

        // Store metadata if needed (could be extended to save to a database)
        if (metadata != null && !metadata.isEmpty()) {
            log.info("Associated metadata: {}", metadata);
            // Implementation for metadata storage could be added here
        }

        log.info("File stored successfully at: {}", targetPath);
        return targetPath.toString();
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    @Override
    public InputStream retrieve(String identifier) throws IOException {
        log.info("Retrieving file with identifier: {}", identifier);
        InputStream inputStream = Files.newInputStream(Paths.get(identifier));
        log.info("File retrieved successfully: {}", identifier);
        return inputStream;
    }

    @Override
    public boolean delete(String identifier) throws IOException {
        log.info("Deleting file with identifier: {}", identifier);
        boolean deleted = Files.deleteIfExists(Paths.get(identifier));
        if (deleted) {
            log.info("File deleted successfully: {}", identifier);
            return deleted;
        } else {
            log.warn("File not found for deletion: {}", identifier);
        }
        return false;
    }

    @Override
    public String getStorageType() {
        log.info("Retrieving storage type: filesystem");
        return "filesystem";
    }
}