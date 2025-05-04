package com.enit.satellite_platform.modules.user_management.management_cvore_service.security.device.controller;

import com.enit.satellite_platform.modules.user_management.management_cvore_service.security.device.DeviceManagementService;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.security.device.dto.DeviceInfoDTO;
import com.enit.satellite_platform.shared.dto.GenericResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@Tag(name = "Device Management", description = "APIs for managing user devices and sessions")
@RequiredArgsConstructor
public class DeviceManagementController {

    private final DeviceManagementService deviceManagementService;

    @Operation(summary = "Get all user devices")
    @ApiResponse(responseCode = "200", description = "List of user's devices")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DeviceInfoDTO>> getUserDevices(Authentication authentication,
                                                            @RequestParam(required = false) String currentDeviceId) {
        String userId = authentication.getName();
        return ResponseEntity.ok(deviceManagementService.getUserDevices(userId, currentDeviceId));
    }

    @Operation(summary = "Get active user devices")
    @ApiResponse(responseCode = "200", description = "List of user's active devices")
    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DeviceInfoDTO>> getActiveDevices(Authentication authentication,
                                                              @RequestParam(required = false) String currentDeviceId) {
        String userId = authentication.getName();
        return ResponseEntity.ok(deviceManagementService.getActiveDevices(userId, currentDeviceId));
    }

    @Operation(summary = "Approve a device")
    @ApiResponse(responseCode = "200", description = "Device approved successfully")
    @PostMapping("/{deviceId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse<Void>> approveDevice(@PathVariable String deviceId,
                                                             Authentication authentication) {
        deviceManagementService.approveDevice(deviceId, authentication.getName());
        return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Device approved successfully"));
    }

    @Operation(summary = "Revoke a device")
    @ApiResponse(responseCode = "200", description = "Device revoked successfully")
    @PostMapping("/{deviceId}/revoke")
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isDeviceOwner(#deviceId, principal.username)")
    public ResponseEntity<GenericResponse<Void>> revokeDevice(@PathVariable String deviceId,
                                                            Authentication authentication) {
        deviceManagementService.revokeDevice(deviceId, authentication.getName());
        return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Device revoked successfully"));
    }

    @Operation(summary = "Cleanup inactive devices")
    @ApiResponse(responseCode = "200", description = "Inactive devices cleaned up successfully")
    @PostMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse<Void>> cleanupInactiveDevices() {
        deviceManagementService.cleanupInactiveDevices();
        return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Inactive devices cleaned up successfully"));
    }
}
