package com.enit.satellite_platform.modules.project_management.dto;

import java.util.List;

import com.enit.satellite_platform.modules.resource_management.image_management.dto.ImageDTO;

import lombok.Data;

@Data
public class ProjectDto {
    private String id;
    private String projectName;
    private String description;
    private String ownerEmail;
    private List<ImageDTO> images;

}
