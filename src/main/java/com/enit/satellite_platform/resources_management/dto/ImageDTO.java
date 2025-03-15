package com.enit.satellite_platform.resources_management.dto;

import lombok.Data;

@Data
public class ImageDTO {
    private String imageId;
    private String projectId;
    private String imageName;
    private String downloadUrl;
    private String previewUrl;
    private String imagePath;
}
