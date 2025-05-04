package com.enit.satellite_platform.modules.project_management.dto;

import com.enit.satellite_platform.modules.project_management.entities.PermissionLevel;
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
    private String otherEmail;
    /**
     * The permission level to grant to the user.
     */
    private PermissionLevel permission;
}
