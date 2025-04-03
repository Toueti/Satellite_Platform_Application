package com.enit.satellite_platform.modules.project_management.dto;

import lombok.Data;

/**
 * DTO for creating a new project.
 */
@Data
public class CreateProjectRequest {

    /**
     * The name of the project.
     */
    private String projectNAme;
    /**
     * The name of the project owner.
     */
    private String projectOwnerName;

}
