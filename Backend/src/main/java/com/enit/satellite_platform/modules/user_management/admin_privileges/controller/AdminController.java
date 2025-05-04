 package com.enit.satellite_platform.modules.user_management.admin_privileges.controller;

import com.enit.satellite_platform.config.dto.ManageablePropertyDto;
import com.enit.satellite_platform.config.dto.UpdatePropertyRequestDto;
import com.enit.satellite_platform.exceptions.DuplicationException;
import com.enit.satellite_platform.modules.user_management.admin_privileges.services.AdminServices;
import com.enit.satellite_platform.modules.user_management.admin_privileges.services.ConfigManagementService;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.AdminSignupRequest;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.User;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.exceptions.RoleNotFoundException;
import com.enit.satellite_platform.shared.dto.GenericResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema; // Added import
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminServices adminServices;

    @Autowired
    private ConfigManagementService configManagementService;

    // --- Configuration Management Endpoints ---

    @Operation(summary = "Retrieve all manageable configuration properties",
            description = "Fetches a list of configuration properties that can be managed by administrators, including current values, defaults, and metadata.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved manageable properties",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ManageablePropertyDto.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User lacks ADMIN authority",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class)))
    })
    @GetMapping("/config/manageable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ManageablePropertyDto>> getManageableProperties() {
        List<ManageablePropertyDto> properties = configManagementService.getManageableProperties();
        return ResponseEntity.ok(properties);
    }

    @Operation(summary = "Update a manageable configuration property",
            description = "Updates a specific configuration property designated as manageable. A null value resets the property to its default.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Property updated or reset successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - Invalid key or validation error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User lacks ADMIN authority",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class)))
    })
    @PutMapping("/config/manageable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse<?>> updateManageableProperty(@Valid @RequestBody UpdatePropertyRequestDto updateRequest) {
        try {
            configManagementService.updateProperty(updateRequest);
            String message = (updateRequest.getValue() == null)
                    ? "Property '" + updateRequest.getKey() + "' reset to default successfully."
                    : "Property '" + updateRequest.getKey() + "' updated successfully.";
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", message));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new GenericResponse<>("BAD_REQUEST", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new GenericResponse<>("ERROR", "An unexpected error occurred while updating property: " + updateRequest.getKey()));
        }
    }


    // --- User Management Endpoints ---

    @Operation(summary = "Create a new user",
            description = "Creates a new user with the specified username, email, password, and roles.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - Invalid input or duplication",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User lacks ADMIN authority",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class)))
    })
    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam Set<String> roles) {
        try {
            User user = adminServices.createUser(username, email, password, roles);
            return ResponseEntity.ok(user);
        } catch (DuplicationException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new GenericResponse<>("BAD_REQUEST", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new GenericResponse<>("ERROR", "Failed to create user: " + e.getMessage()));
        }
    }

    @Operation(summary = "Update an existing user",
            description = "Updates a user's username, email, and roles by their ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - Invalid input or duplication",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User lacks ADMIN authority",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class)))
    })
    @PutMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(
            @PathVariable String userId,
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam Set<String> roles) {
        try {
            User updatedUser = adminServices.updateUser(new ObjectId(userId), username, email, roles);
            return ResponseEntity.ok(updatedUser);
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(404).body(new GenericResponse<>("NOT_FOUND", e.getMessage()));
        } catch (DuplicationException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new GenericResponse<>("BAD_REQUEST", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new GenericResponse<>("ERROR", "Failed to update user: " + e.getMessage()));
        }
    }

    @Operation(summary = "Reset a user's password",
            description = "Resets the password for a user specified by their ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - Invalid input",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User lacks ADMIN authority",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class)))
    })
    @PostMapping("/users/{userId}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse<?>> resetUserPassword(
            @PathVariable String userId,
            @RequestParam String newPassword) {
        try {
            adminServices.resetUserPassword(new ObjectId(userId), newPassword);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Password reset successfully for user: " + userId));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(404).body(new GenericResponse<>("NOT_FOUND", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new GenericResponse<>("BAD_REQUEST", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new GenericResponse<>("ERROR", "Failed to reset password: " + e.getMessage()));
        }
    }

    @Operation(summary = "Delete a user",
            description = "Deletes a user by their ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User deleted successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - Invalid ID",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User lacks ADMIN authority",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class)))
    })
    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse<?>> deleteUser(@PathVariable String userId) {
        try {
            adminServices.deleteUser(userId);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "User deleted successfully: " + userId));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(404).body(new GenericResponse<>("NOT_FOUND", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new GenericResponse<>("BAD_REQUEST", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new GenericResponse<>("ERROR", "Failed to delete user: " + e.getMessage()));
        }
    }

    @Operation(summary = "Retrieve all users",
            description = "Fetches a list of all users in the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all users",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User lacks ADMIN authority",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class)))
    })
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = adminServices.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new GenericResponse<>("ERROR", "Failed to retrieve users: " + e.getMessage()));
        }
    }

    @Operation(summary = "Lock a user account",
            description = "Locks a user account by disabling it.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User account locked successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - Invalid ID",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User lacks ADMIN authority",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class)))
    })
    @PostMapping("/users/{userId}/{lock}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse<?>> lockUser(@PathVariable String userId, @PathVariable boolean lock) {
        if (userId == null || userId.isBlank() || !ObjectId.isValid(userId)) {
            return ResponseEntity.badRequest().body(new GenericResponse<>("BAD_REQUEST", "User ID cannot be null or blank"));
        }
        try {
            adminServices.lockUnlockUserAccount(userId, lock);
            if (lock) {
                return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "User account locked successfully: " + userId));
            } else {
                return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "User account unlocked successfully: " + userId));
            }
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(404).body(new GenericResponse<>("NOT_FOUND", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new GenericResponse<>("BAD_REQUEST", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new GenericResponse<>("ERROR", "Failed to lock user account: " + e.getMessage()));
        }
    }

    // --- Admin Signup Request Management Endpoints ---

    @Operation(summary = "Get pending admin signup requests",
            description = "Retrieves a list of all admin signup requests that are currently pending approval.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved pending requests",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = AdminSignupRequest.class)))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User lacks ADMIN role",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class)))
    })
    @GetMapping("/signup-requests/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminSignupRequest>> getPendingAdminRequests() {
        try {
            List<AdminSignupRequest> requests = adminServices.getPendingAdminRequests();
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            // Log the exception details
            return ResponseEntity.internalServerError()
                    .body(null); // Avoid returning GenericResponse here as the expected type is List
        }
    }

    @Operation(summary = "Approve an admin signup request",
            description = "Approves a pending admin signup request, creating the user with ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Admin request approved successfully, user created",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - e.g., user already exists",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User lacks ADMIN role",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "404", description = "Request not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - e.g., request not pending",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class)))
    })
    @PostMapping("/signup-requests/{requestId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveAdminRequest(@PathVariable String requestId) {
        try {
            User newUser = adminServices.approveAdminRequest(requestId);
            return ResponseEntity.ok(newUser);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new GenericResponse<>("NOT_FOUND", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new GenericResponse<>("CONFLICT", e.getMessage()));
        } catch (DuplicationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new GenericResponse<>("BAD_REQUEST", e.getMessage()));
        } catch (RoleNotFoundException e) {
             // This indicates a critical configuration error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new GenericResponse<>("CONFIG_ERROR", e.getMessage()));
        } catch (Exception e) {
            // Log the exception details
            return ResponseEntity.internalServerError()
                    .body(new GenericResponse<>("ERROR", "An unexpected error occurred while approving request: " + requestId));
        }
    }

    @Operation(summary = "Reject an admin signup request",
            description = "Rejects a pending admin signup request.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Admin request rejected successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User lacks ADMIN role",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "404", description = "Request not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - e.g., request not pending",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenericResponse.class)))
    })
    @PostMapping("/signup-requests/{requestId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse<?>> rejectAdminRequest(@PathVariable String requestId) {
        try {
            adminServices.rejectAdminRequest(requestId);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Admin signup request rejected successfully: " + requestId));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new GenericResponse<>("NOT_FOUND", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new GenericResponse<>("CONFLICT", e.getMessage()));
        } catch (Exception e) {
            // Log the exception details
            return ResponseEntity.internalServerError()
                    .body(new GenericResponse<>("ERROR", "An unexpected error occurred while rejecting request: " + requestId));
        }
    }
}
