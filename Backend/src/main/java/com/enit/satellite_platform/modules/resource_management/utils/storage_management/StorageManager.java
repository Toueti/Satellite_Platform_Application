package com.enit.satellite_platform.modules.resource_management.utils.storage_management;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Component
public class StorageManager {

    private static final Logger log = LoggerFactory.getLogger(StorageManager.class);

    private final List<StorageService> storageServices;
    private final StorageService defaultStorageService; // Default to GridFS, for example

    public StorageManager(List<StorageService> storageServices) {
        this.storageServices = storageServices;
        this.defaultStorageService = storageServices.stream()
            .filter(s -> s.getStorageType().equals("tmp-filesystem"))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Default tmp-filesystem storage service not found"));
    }

    public StorageService getDefaultStorageService() {
        return defaultStorageService;
    }

    public String store(MultipartFile file, Map<String, Object> metadata, String storageType) throws IOException {
        StorageService service = getServiceByType(storageType);
        return service.store(file, metadata);
    }

    public InputStream retrieve(String identifier, String storageType) throws IOException {
        StorageService service = getServiceByType(storageType);
        return service.retrieve(identifier);
    }

    public void delete(String identifier, String storageType) throws IOException {
        StorageService service = getServiceByType(storageType);
        service.delete(identifier);
    }

    private StorageService getServiceByType(String storageType) {
        log.debug("Finding storage service for type: {}", storageType);
        StorageService service = storageServices.stream()
            .filter(s -> s.getStorageType().equalsIgnoreCase(storageType))
            .findFirst()
            .orElse(defaultStorageService);
        log.debug("Selected storage service: {}", service.getStorageType());
        return service;
    }
}