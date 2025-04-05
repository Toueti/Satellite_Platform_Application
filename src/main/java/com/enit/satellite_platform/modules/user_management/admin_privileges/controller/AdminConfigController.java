package com.enit.satellite_platform.modules.user_management.admin_privileges.controller;

import com.enit.satellite_platform.config.dto.ManageablePropertyDto;
import com.enit.satellite_platform.config.dto.UpdatePropertyRequestDto;
import com.enit.satellite_platform.config.model.ConfigProperty;
import com.enit.satellite_platform.modules.user_management.admin_privileges.services.ConfigManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/config") // Base path for admin config endpoints
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')") // Secure the entire controller for ADMIN role
public class AdminConfigController {

    private final ConfigManagementService configManagementService;

    /**
     * Retrieves all manageable configuration properties with their current and default values.
     *
     * @return List of manageable properties.
     */
    @GetMapping
    public ResponseEntity<List<ManageablePropertyDto>> getAllManageableProperties() {
        log.info("Admin request to get all manageable properties.");
        List<ManageablePropertyDto> properties = configManagementService.getManageableProperties();
        return ResponseEntity.ok(properties);
    }

    /**
     * Updates a single configuration property.
     * The request body should contain the key and the new value.
     * To reset a property to its default, send null as the value.
     *
     * @param request DTO containing the key and new value.
     * @return The updated ConfigProperty object or representation of reset state.
     */
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
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error updating property."));
        }
    }

    /**
     * Updates multiple configuration properties at once.
     * The request body should be a Map where keys are property names and values are the new settings.
     * Sending null as a value for a key will reset that property to its default.
     *
     * @param configs Map of property keys to new values.
     * @return Confirmation message or details of updated properties.
     */
    @PostMapping("/batch")
    public ResponseEntity<?> updateMultipleProperties(@RequestBody Map<String, String> configs) {
        log.info("Admin request to batch update {} properties.", configs.size());
        try {
            // Iterate and update each property individually using the existing service method
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
                    "updatedCount", updatedProperties.size()
            ));
        } catch (IllegalArgumentException e) {
            log.error("Failed during batch property update: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Internal server error during batch property update", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error during batch update."));
        }
    }

    // Note: The user's example used getAllConfigs("messaging") and updateConfigs("messaging", configs).
    // The current ConfigManagementService doesn't seem to have methods supporting a prefix filter like "messaging".
    // The implemented GET / retrieves all manageable properties, and POST /batch updates specified properties.
    // If prefix-based management is strictly needed, ConfigManagementService would need modification.
}
