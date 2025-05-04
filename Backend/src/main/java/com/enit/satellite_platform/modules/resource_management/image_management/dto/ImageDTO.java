package com.enit.satellite_platform.modules.resource_management.image_management.dto;

import java.util.Map;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ImageDTO {
    private String imageId;
    private String projectId;
    private String imageName;
    private Map<String, Object> metadata;
    private MultipartFile file;
    private long fileSize;
    private String storageType;
    private String storageIdentifier; // Generic identifier (could be GridFS ID, file path, S3 URI, etc.)
}
