package com.enit.satellite_platform.modules.user_management.admin_privileges.controller;

import com.enit.satellite_platform.config.dto.ManageablePropertyDto;
import com.enit.satellite_platform.config.dto.UpdatePropertyRequestDto;
import com.enit.satellite_platform.config.model.ConfigProperty;
import com.enit.satellite_platform.modules.user_management.admin_privileges.services.ConfigManagementService;
import com.enit.satellite_platform.shared.dto.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/config") // Base path for admin config endpoints
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')") // Secure the entire controller for ADMIN role
public class AdminConfigController {

    private final ConfigManagementService configManagementService;

    /**
     * Retrieves all manageable configuration properties with their current and
     * default values.
     *
     * @return List of manageable properties.
     */
    @Operation(summary = "Retrieve all manageable configuration properties", description = "Fetches all manageable configuration properties with their current and default values.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of manageable properties retrieved successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ManageablePropertyDto.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User lacks ADMIN authority", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @GetMapping
    public ResponseEntity<List<ManageablePropertyDto>> getAllManageableProperties() {
        log.info("Admin request to get all manageable properties.");
        List<ManageablePropertyDto> properties = configManagementService.getManageableProperties();
        return ResponseEntity.ok(properties);
    }

    /**
     * Retrieves a specific manageable property by its prefix.
     *
     * @param prefix The prefix of the property to retrieve.
     * @return The requested manageable property or a 404 if not found.
     */
    @Operation(summary = "Retrieve a specific manageable property by prefix", description = "Fetches a specific manageable property by its prefix.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Property retrieved successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ManageablePropertyDto.class))),
            @ApiResponse(responseCode = "404", description = "Property not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "403", description = "Forbidden - User lacks ADMIN authority", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @GetMapping("/{prefix}")
    public ResponseEntity<?> getManageableProperty(@PathVariable String prefix) {
        log.info("Admin request to get property '{}'", prefix);
        List<ManageablePropertyDto> property = configManagementService.getAllConfigs(prefix);
        if (property != null) {
            return ResponseEntity.ok(property);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Updates a single configuration property.
     * The request body should contain the key and the new value.
     * To reset a property to its default, send null as the value.
     *
     * @param request DTO containing the key and new value.
     * @return The updated ConfigProperty object or representation of reset state.
     */
    @Operation(summary = "Update a single configuration property", description = "Updates a single configuration property. Send null as the value to reset the property to its default.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Property updated successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ConfigProperty.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - Invalid key or value", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "403", description = "Forbidden - User lacks ADMIN authority", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @PutMapping
    public ResponseEntity<?> updateSingleProperty(@RequestBody UpdatePropertyRequestDto request) {
        log.info("Admin request to update property '{}' to value '{}'", request.getKey(), request.getValue());
        try {
            ConfigProperty updatedProperty = configManagementService.updateProperty(request);
            return ResponseEntity.ok(updatedProperty);
        } catch (IllegalArgumentException e) {
            log.error("Failed to update property '{}': {}", request.getKey(), e.getMessage());
            // Return a more informative error response
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Internal server error updating property '{}'", request.getKey(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal server error updating property."));
        }
    }

    /**
     * Updates multiple configuration properties at once.
     * The request body should be a Map where keys are property names and values are
     * the new settings.
     * Sending null as a value for a key will reset that property to its default.
     *
     * @param configs Map of property keys to new values.
     * @return Confirmation message or details of updated properties.
     */
    @Operation(summary = "Batch update configuration properties", description = "Updates multiple configuration properties at once. Send null as the value for a key to reset that property to its default.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Batch update processed successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "400", description = "Bad request - Invalid keys or values", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "403", description = "Forbidden - User lacks ADMIN authority", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @PostMapping("/batch")
    public ResponseEntity<?> updateMultipleProperties(@RequestBody Map<String, String> configs) {
        log.info("Admin request to batch update {} properties.", configs.size());
        try {
            // Iterate and update each property individually using the existing service
            // method
            List<ConfigProperty> updatedProperties = configs.entrySet().stream()
                    .map(entry -> {
                        UpdatePropertyRequestDto requestDto = new UpdatePropertyRequestDto();
                        requestDto.setKey(entry.getKey());
                        requestDto.setValue(entry.getValue());
                        return configManagementService.updateProperty(requestDto);
                    })
                    .collect(Collectors.toList());

            // Could return the list of updated properties or just a success message
            return ResponseEntity.ok(Map.of(
                    "message", "Batch configuration update processed.",
                    "updatedCount", updatedProperties.size()));
        } catch (IllegalArgumentException e) {
            log.error("Failed during batch property update: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Internal server error during batch property update", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal server error during batch update."));
        }
    }

    @Operation(summary = "Update a configuration property (legacy)", description = "Updates a configuration property in the runtime environment using AdminServices. Note: This is a legacy endpoint; prefer /config/manageable for persistent updates.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Property updated successfully in runtime environment", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - Invalid key", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User lacks ADMIN authority", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericResponse.class)))
    })
    @PutMapping("/config/runtime")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse<?>> updateRuntimeProperty(
            @Valid @RequestBody UpdatePropertyRequestDto updateRequest) {
        try {
            configManagementService.updateConfigurationProperty(updateRequest);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS",
                    "Runtime property '" + updateRequest.getKey() + "' updated successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new GenericResponse<>("BAD_REQUEST", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new GenericResponse<>("ERROR",
                            "An unexpected error occurred while updating runtime property: " + updateRequest.getKey()));
        }
    }

}
