package com.enit.satellite_platform.modules.project_management.dto;

import java.util.Date;
import java.util.Set;

import lombok.Data;

@Data
public class ProjectDto {
    private String id;
    private String projectName;
    private String description;
    private String status;
    private String ownerEmail;
    Set<String> tags;
    private Date createdAt;
    private Date updatedAt;
    private Date lastAccessedTime;

}
