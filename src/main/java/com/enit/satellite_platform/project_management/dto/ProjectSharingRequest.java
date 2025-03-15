package com.enit.satellite_platform.project_management.dto;

import lombok.Data;

/**
 * DTO for requests to share a project with another user.
 */
@Data
public class ProjectSharingRequest {
    /**
     * The ID of the project to be shared.
     */
    private String projectId;
    /**
     * The email of the user to share the project with.
     */
    private String otherEmail;  // Email of the user to share with
}
