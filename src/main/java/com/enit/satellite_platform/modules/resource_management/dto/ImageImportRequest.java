package com.enit.satellite_platform.modules.resource_management.dto;

import java.util.List;

import lombok.Data;

@Data
public class ImageImportRequest {
    private String sourceProjectId;
    private String targetProjectId;
    private List<String> imageIds;
}
