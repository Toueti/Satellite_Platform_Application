package com.enit.satellite_platform.project_management.controller;

import com.enit.satellite_platform.exception.DuplicateNameException;
import com.enit.satellite_platform.project_management.dto.GenericResponse;
import com.enit.satellite_platform.project_management.dto.ProjectSharingRequest;
import com.enit.satellite_platform.project_management.dto.ProjectStatisticsDto;
import com.enit.satellite_platform.project_management.exception.ProjectNotFoundException;
import com.enit.satellite_platform.project_management.model.Project;
import com.enit.satellite_platform.project_management.service.ProjectService;
import com.enit.satellite_platform.user_management.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/thematician/projects")
@CrossOrigin(origins = "*")
public class ProjectController {

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
            Project project = projectService.renameProject(new ObjectId(id), newName, email);
            return ResponseEntity.ok(project);
        } catch (DuplicateNameException e) {
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
    @GetMapping("/{projectID}")
    public ResponseEntity<GenericResponse<?>> getProject(@PathVariable String projectID) {
        try {
            Project project = projectService.getProject(new ObjectId(projectID));
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
    public ResponseEntity<GenericResponse<?>> getAllProjects() {
        try {
            String email = getCurrentEmail();
            List<Project> projects = projectService.getAllProjects(email);
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
    @PutMapping("/{projectID}")
    public ResponseEntity<GenericResponse<?>> updateProject(@PathVariable String projectID, @RequestBody Project project) {
        try {
            Project updatedProject = projectService.updateProject(new ObjectId(projectID), project);
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
            @ApiResponse(responseCode = "500", description = "Error deleting project")
    })
    @DeleteMapping("/{projectID}")
    public ResponseEntity<GenericResponse<?>> deleteProject(@PathVariable String projectID) {
        try {
            projectService.deleteProject(new ObjectId(projectID));
            String message = String.format("Project with id: %s deleted successfully", projectID);
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
            Project project = projectService.shareProject(projectId, otherEmail, currentEmail);
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
            Project project = projectService.unshareProject(projectId, otherEmail, currentEmail);
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
    @GetMapping("/{projectID}/shared-users")
    public ResponseEntity<GenericResponse<?>> getSharedUsers(@PathVariable String projectID) {
        try {
            String email = getCurrentEmail();
            Set<User> sharedUsers = projectService.getSharedUsers(new ObjectId(projectID), email);
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
    public ResponseEntity<GenericResponse<?>> getSharedWithMe() {
        try {
            String email = getCurrentEmail();
            List<Project> sharedProjects = projectService.getSharedWithMe(email);
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
    @PostMapping("/{projectID}/archive")
    public ResponseEntity<GenericResponse<?>> archiveProject(@PathVariable String projectID) {
        try {
            String email = getCurrentEmail();
            Project project = projectService.archiveProject(new ObjectId(projectID), email);
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
    @PostMapping("/{projectID}/unarchive")
    public ResponseEntity<GenericResponse<?>> unarchiveProject(@PathVariable String projectID) {
        try {
            String email = getCurrentEmail();
            Project project = projectService.unarchiveProject(new ObjectId(projectID), email);
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
    public ResponseEntity<GenericResponse<?>> getArchivedProjects() {
        try {
            String email = getCurrentEmail();
            List<Project> projects = projectService.getArchivedProjects(email);
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            if (page < 0 || size <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new GenericResponse<>("FAILURE", "Page must be non-negative and size must be positive", null));
            }
            String email = getCurrentEmail();
            List<Project> projects = projectService.searchProjects(email, query, page, size);
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
    @PostMapping("/{projectID}/tags")
    public ResponseEntity<GenericResponse<?>> addTagToProject(
            @PathVariable String projectID,
            @RequestParam String tag) {
        try {
            String email = getCurrentEmail();
            Project project = projectService.addTagToProject(new ObjectId(projectID), tag, email);
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
    public ResponseEntity<GenericResponse<?>> getProjectsByTag(@RequestParam String tag) {
        try {
            String email = getCurrentEmail();
            List<Project> projects = projectService.getProjectsByTag(email, tag);
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
    @PostMapping("/{projectID}/duplicate")
    public ResponseEntity<GenericResponse<?>> duplicateProject(
            @PathVariable String projectID,
            @RequestParam String newName) {
        try {
            if (newName == null || newName.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new GenericResponse<>("FAILURE", "New project name cannot be empty", null));
            }
            String email = getCurrentEmail();
            Project project = projectService.duplicateProject(new ObjectId(projectID), newName, email);
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
    @PutMapping("/{projectID}/status")
    public ResponseEntity<GenericResponse<?>> updateProjectStatus(
            @PathVariable String projectID,
            @RequestParam String status) {
        try {
            String email = getCurrentEmail();
            Project project = projectService.updateProjectStatus(new ObjectId(projectID), status, email);
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
    public ResponseEntity<GenericResponse<?>> getProjectsByStatus(@RequestParam String status) {
        try {
            String email = getCurrentEmail();
            List<Project> projects = projectService.getProjectsByStatus(email, status);
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
    @GetMapping("/{projectID}/export")
    public ResponseEntity<GenericResponse<?>> exportProject(@PathVariable String projectID) {
        try {
            String email = getCurrentEmail();
            Map<String, Object> exportData = projectService.exportProject(new ObjectId(projectID), email);
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
    public ResponseEntity<GenericResponse<?>> bulkDeleteProjects(@RequestBody List<String> projectIDs) {
        try {
            String email = getCurrentEmail();
            List<ObjectId> ids = projectIDs.stream().map(ObjectId::new).toList();
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
}