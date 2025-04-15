package com.enit.satellite_platform.modules.project_management.controllers;

import com.enit.satellite_platform.shared.dto.GenericResponse;
import com.enit.satellite_platform.exceptions.DuplicationException;
import com.enit.satellite_platform.modules.project_management.dto.ProjectDto;
import com.enit.satellite_platform.modules.project_management.dto.ProjectSharingRequest;
import com.enit.satellite_platform.modules.project_management.dto.ProjectStatisticsDto;
import com.enit.satellite_platform.modules.project_management.entities.PermissionLevel;
import com.enit.satellite_platform.modules.project_management.entities.Project;
import com.enit.satellite_platform.modules.project_management.exceptions.ProjectNotFoundException;
import com.enit.satellite_platform.modules.project_management.services.ProjectService;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/thematician/projects")
@CrossOrigin(origins = "*")
public class ProjectController {

    // private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);

    @Autowired
    private ProjectService projectService;

    // Helper method to extract email from authentication
    private String getCurrentEmail() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (principal instanceof UserDetails)
                ? ((UserDetails) principal).getUsername()
                : principal.toString();
    }

    @Operation(summary = "Create a new project")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid project data"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Error creating project")
    })
    @PostMapping("/create")
    public ResponseEntity<GenericResponse<?>> createProject(@RequestBody Project project) {
        try {
            String email = getCurrentEmail();
            Project createdProject = projectService.createProject(project, email);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Project created successfully", createdProject));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error creating project: " + e.getMessage(), null));
        }
    }


    @PutMapping("/{id}/rename")
    public ResponseEntity<?> renameProject(@PathVariable String id, @RequestParam String newName) {
        try {
            String email = getCurrentEmail();
            ProjectDto project = projectService.renameProject(new ObjectId(id), newName, email);
            return ResponseEntity.ok(project);
        } catch (DuplicationException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error renaming project: " + e.getMessage());
        }
    }

    @Operation(summary = "Get project statistics for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Error retrieving statistics")
    })
    @GetMapping("/statistics")
    public ResponseEntity<GenericResponse<?>> getStatistics() {
        try {
            String email = getCurrentEmail();
            ProjectStatisticsDto statistics = projectService.getStatistics(email);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Statistics retrieved successfully", statistics));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error retrieving statistics: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Get a specific project by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "500", description = "Error retrieving project")
    })
    @GetMapping("/{projectId}")
    public ResponseEntity<GenericResponse<?>> getProject(@PathVariable String projectId) {
        try {
            ProjectDto project = projectService.getProject(new ObjectId(projectId));
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Project retrieved successfully", project));
        } catch (ProjectNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error retrieving project: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Get a specific project by name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "500", description = "Error retrieving project")
    })
    @GetMapping("/name/{projectName}")
    public ResponseEntity<GenericResponse<?>> getProjectByName(@PathVariable String projectName) {
        try {
            ProjectDto project = projectService.getProjectByName(projectName);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Project retrieved successfully", project));
        } catch (ProjectNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error retrieving project: " + e.getMessage(), null));
        }
    }


    @Operation(summary = "Get all projects for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Projects retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Error retrieving projects")
    })
    @GetMapping("/all")
    public ResponseEntity<GenericResponse<?>> getAllProjects(Pageable pageable) {
        try {
            String email = getCurrentEmail();
            Page<ProjectDto> projects = projectService.getAllProjects(email,pageable);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Projects retrieved successfully", projects));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error retrieving projects: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Update a specific project by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project updated successfully"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "500", description = "Error updating project")
    })
    @PutMapping("/{projectId}")
    public ResponseEntity<GenericResponse<?>> updateProject(@PathVariable String projectId, @RequestBody Project project) {
        try {
            ProjectDto updatedProject = projectService.updateProject(new ObjectId(projectId), project);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Project updated successfully", updatedProject));
        } catch (ProjectNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error updating project: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Delete a specific project by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "500", description = "Error soft deleting project")
    })
    @DeleteMapping("/{projectId}")
    public ResponseEntity<GenericResponse<?>> deleteProject(@PathVariable String projectId) { // This now performs soft delete
        try {
            getCurrentEmail(); // Need email for ownership check if required by service layer (though current service impl doesn't use it for soft delete)
            projectService.deleteProject(new ObjectId(projectId)); // Calls the soft delete method
            String message = String.format("Project with id: %s soft deleted successfully", projectId);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", message));
        } catch (ProjectNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error deleting project: " + e.getMessage(), null));
        }
    }
    
    @Operation(summary = "Share a project with another user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project shared successfully"),
            @ApiResponse(responseCode = "403", description = "User not authorized to share"),
            @ApiResponse(responseCode = "404", description = "Project or user not found"),
            @ApiResponse(responseCode = "500", description = "Error sharing project")
    })
    @PostMapping("/share")
    public ResponseEntity<GenericResponse<?>> shareProject(@RequestBody ProjectSharingRequest request) {
        try {
            String currentEmail = getCurrentEmail();
            String projectId = request.getProjectId();
            String otherEmail = request.getOtherEmail();
            String permission = request.getPermission();

            // Default to VIEWER if permission is not provided
            if (permission == null || permission.trim().isEmpty()) {
                permission = "VIEWER";
            }

            ProjectDto project = projectService.shareProject(projectId, otherEmail, currentEmail, PermissionLevel.valueOf(permission.toUpperCase()));
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Project shared successfully", project));
        } catch (ProjectNotFoundException | UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error sharing project: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Unshare a project with another user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project unshared successfully"),
            @ApiResponse(responseCode = "403", description = "User not authorized to unshare"),
            @ApiResponse(responseCode = "404", description = "Project or user not found"),
            @ApiResponse(responseCode = "500", description = "Error unsharing project")
    })
    @PostMapping("/unshare")
    public ResponseEntity<GenericResponse<?>> unshareProject(@RequestBody ProjectSharingRequest request) {
        try {
            String currentEmail = getCurrentEmail();
            String projectId = request.getProjectId();
            String otherEmail = request.getOtherEmail();
            ProjectDto project = projectService.unshareProject(projectId, otherEmail, currentEmail);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Project unshared successfully", project));
        } catch (ProjectNotFoundException | UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error unsharing project: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Get users a project is shared with")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shared users retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "User not authorized"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "500", description = "Error retrieving shared users")
    })
    @GetMapping("/{projectId}/shared-users")
    public ResponseEntity<GenericResponse<?>> getSharedUsers(@PathVariable String projectId) {
        try {
            String email = getCurrentEmail();
            Set<User> sharedUsers = projectService.getSharedUsers(new ObjectId(projectId), email);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Shared users retrieved successfully", sharedUsers));
        } catch (ProjectNotFoundException | UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error retrieving shared users: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Get projects shared with the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shared projects retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Error retrieving shared projects")
    })
    @GetMapping("/shared-with-me")
    public ResponseEntity<GenericResponse<?>> getSharedWithMe(Pageable pageable) {
        try {
            String email = getCurrentEmail();
            Page<ProjectDto> sharedProjects = projectService.getSharedWithMe(email, pageable);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Shared projects retrieved successfully", sharedProjects));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error retrieving shared projects: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Get the last n accessed projects for the user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Last accessed projects retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid number of projects requested"),
            @ApiResponse(responseCode = "500", description = "Error retrieving last accessed projects")
    })
    @GetMapping("/last-accessed")
    public ResponseEntity<GenericResponse<?>> getLastAccessedProjects(@RequestParam int n) {
        try {
            if (n <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new GenericResponse<>("FAILURE", "Number of projects must be positive", null));
            }
            String email = getCurrentEmail();
            List<Project> projects = projectService.getLastAccessedProjects(email, n);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Last accessed projects retrieved successfully", projects));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error retrieving last accessed projects: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Archive a project")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project archived successfully"),
            @ApiResponse(responseCode = "403", description = "User not authorized"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "500", description = "Error archiving project")
    })
    @PostMapping("/{projectId}/archive")
    public ResponseEntity<GenericResponse<?>> archiveProject(@PathVariable String projectId) {
        try {
            String email = getCurrentEmail();
            ProjectDto project = projectService.archiveProject(new ObjectId(projectId), email);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Project archived successfully", project));
        } catch (ProjectNotFoundException | UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error archiving project: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Unarchive a project")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project unarchived successfully"),
            @ApiResponse(responseCode = "403", description = "User not authorized"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "400", description = "Project is not archived"),
            @ApiResponse(responseCode = "500", description = "Error unarchiving project")
    })
    @PostMapping("/{projectId}/unarchive")
    public ResponseEntity<GenericResponse<?>> unarchiveProject(@PathVariable String projectId) {
        try {
            String email = getCurrentEmail();
            ProjectDto project = projectService.unarchiveProject(new ObjectId(projectId), email);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Project unarchived successfully", project));
        } catch (ProjectNotFoundException | UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error unarchiving project: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Get archived projects for the user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Archived projects retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Error retrieving archived projects")
    })
    @GetMapping("/archived")
    public ResponseEntity<GenericResponse<?>> getArchivedProjects(Pageable pageable) {
        try {
            String email = getCurrentEmail();
            Page<ProjectDto> projects = projectService.getArchivedProjects(email, pageable);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Archived projects retrieved successfully", projects));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error retrieving archived projects: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Search projects by name or description")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Projects retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Error searching projects")
    })
    @GetMapping("/search")
    public ResponseEntity<GenericResponse<?>> searchProjects(
            @RequestParam String query,
            Pageable pageable) {
        try {
            String email = getCurrentEmail();
            Page<ProjectDto> projects = projectService.searchProjects(email, query, pageable);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Projects retrieved successfully", projects));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error searching projects: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Add a tag to a project")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tag added successfully"),
            @ApiResponse(responseCode = "403", description = "User not authorized"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "500", description = "Error adding tag")
    })
    @PostMapping("/{projectId}/tags")
    public ResponseEntity<GenericResponse<?>> addTagToProject(
            @PathVariable String projectId,
            @RequestParam String tag) {
        try {
            String email = getCurrentEmail();
            ProjectDto project = projectService.tagProject(new ObjectId(projectId), tag, email);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Tag added successfully", project));
        } catch (ProjectNotFoundException | UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error adding tag: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Get projects by tag")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Projects retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Error retrieving projects by tag")
    })
    @GetMapping("/by-tag")
    public ResponseEntity<GenericResponse<?>> getProjectsByTag(@RequestParam String tag, Pageable pageable) {
        try {
            String email = getCurrentEmail();
            Page<ProjectDto> projects = projectService.getProjectsByTag(email, tag, pageable);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Projects retrieved successfully", projects));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error retrieving projects by tag: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Duplicate a project")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project duplicated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid new name"),
            @ApiResponse(responseCode = "403", description = "User not authorized"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "500", description = "Error duplicating project")
    })
    @PostMapping("/{projectId}/duplicate")
    public ResponseEntity<GenericResponse<?>> duplicateProject(
            @PathVariable String projectId,
            @RequestParam String newName) {
        try {
            if (newName == null || newName.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new GenericResponse<>("FAILURE", "New project name cannot be empty", null));
            }
            String email = getCurrentEmail();
            ProjectDto project = projectService.duplicateProject(new ObjectId(projectId), newName, email);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Project duplicated successfully", project));
        } catch (ProjectNotFoundException | UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error duplicating project: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Update project status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project status updated successfully"),
            @ApiResponse(responseCode = "403", description = "User not authorized"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "500", description = "Error updating project status")
    })
    @PutMapping("/{projectId}/status")
    public ResponseEntity<GenericResponse<?>> updateProjectStatus(
            @PathVariable String projectId,
            @RequestParam String status) {
        try {
            String email = getCurrentEmail();
            ProjectDto project = projectService.updateProjectStatus(new ObjectId(projectId), status, email);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Project status updated successfully", project));
        } catch (ProjectNotFoundException | UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error updating project status: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Get projects by status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Projects retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Error retrieving projects by status")
    })
    @GetMapping("/by-status")
    public ResponseEntity<GenericResponse<?>> getProjectsByStatus(@RequestParam String status, Pageable pageable) {
        try {
            String email = getCurrentEmail();
            Page<ProjectDto> projects = projectService.getProjectsByStatus(email, status, pageable);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Projects retrieved successfully", projects));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error retrieving projects by status: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Export project data")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project data exported successfully"),
            @ApiResponse(responseCode = "403", description = "User not authorized"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "500", description = "Error exporting project")
    })
    @GetMapping("/{projectId}/export")
    public ResponseEntity<GenericResponse<?>> exportProject(@PathVariable String projectId) {
        try {
            String email = getCurrentEmail();
            Map<String, Object> exportData = projectService.exportProject(new ObjectId(projectId), email);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Project data exported successfully", exportData));
        } catch (ProjectNotFoundException | UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error exporting project: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Bulk delete projects")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Projects deleted successfully"),
            @ApiResponse(responseCode = "403", description = "User not authorized"),
            @ApiResponse(responseCode = "404", description = "One or more projects not found"),
            @ApiResponse(responseCode = "500", description = "Error deleting projects")
    })
    @DeleteMapping("/bulk-delete")
    public ResponseEntity<GenericResponse<?>> bulkDeleteProjects(@RequestBody List<String> projectIds) {
        try {
            String email = getCurrentEmail();
            List<ObjectId> ids = projectIds.stream().map(ObjectId::new).toList();
            projectService.bulkDeleteProjects(ids, email);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Projects deleted successfully"));
        } catch (ProjectNotFoundException | UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error deleting projects: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Force permanent deletion of a soft-deleted project")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project permanently deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Project is not soft-deleted"),
            @ApiResponse(responseCode = "403", description = "User not authorized"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "500", description = "Error force deleting project")
    })
    @DeleteMapping("/{projectId}/force")
    public ResponseEntity<GenericResponse<?>> forceDeleteProject(@PathVariable String projectId) {
        try {
            String email = getCurrentEmail();
            projectService.forceDeleteProject(new ObjectId(projectId), email);
            String message = String.format("Project with id: %s permanently deleted successfully", projectId);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", message));
        } catch (ProjectNotFoundException | UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error force deleting project: " + e.getMessage(), null));
        }
    }


    @Operation(summary = "Restore a soft-deleted project")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project restored successfully"),
            @ApiResponse(responseCode = "403", description = "User not authorized"),
            @ApiResponse(responseCode = "404", description = "Project not found or not deleted"),
            @ApiResponse(responseCode = "400", description = "Project is not deleted"),
            @ApiResponse(responseCode = "500", description = "Error restoring project")
    })
    @PostMapping("/{projectId}/restore")
    public ResponseEntity<GenericResponse<?>> restoreProject(@PathVariable String projectId) {
        try {
            String email = getCurrentEmail();
            ProjectDto project = projectService.restoreProject(new ObjectId(projectId), email);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Project restored successfully", project));
        } catch (ProjectNotFoundException | UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error restoring project: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Update retention period for a soft-deleted project")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Retention period updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid retention period or project not deleted"),
            @ApiResponse(responseCode = "403", description = "User not authorized"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "500", description = "Error updating retention period")
    })
    @PutMapping("/{projectId}/retention")
    public ResponseEntity<GenericResponse<?>> updateRetentionPeriod(
            @PathVariable String projectId,
            @RequestParam int days) {
        try {
            String email = getCurrentEmail();
            ProjectDto project = projectService.updateRetentionPeriod(new ObjectId(projectId), days, email);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Retention period updated successfully", project));
        } catch (ProjectNotFoundException | UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error updating retention period: " + e.getMessage(), null));
        }
    }

     @Operation(summary = "Get soft-deleted projects for the user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deleted projects retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Error retrieving deleted projects")
    })
    @GetMapping("/deleted")
    public ResponseEntity<GenericResponse<?>> getDeletedProjects(Pageable pageable) {
        try {
            String email = getCurrentEmail();
            // Need to add getDeletedProjects method to ProjectService and ProjectRepository
            Page<ProjectDto> projects = projectService.getDeletedProjects(email, pageable);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Deleted projects retrieved successfully", projects));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error retrieving deleted projects: " + e.getMessage(), null));
        }
    }


    @PostMapping("/createFromTemplate")
    public ResponseEntity<?> createProjectFromTemplate(@RequestParam String templateName,
                                                         @RequestParam String newProjectName,
                                                         Principal principal) {
        try {
            ProjectDto project = projectService.createProjectFromTemplate(templateName, newProjectName, principal.getName());
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Project created from template successfully", project));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error creating project from template: " + e.getMessage(), null));
        }
    }
}
