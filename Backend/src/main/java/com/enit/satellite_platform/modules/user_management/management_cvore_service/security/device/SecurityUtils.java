package com.enit.satellite_platform.modules.user_management.management_cvore_service.security.device;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Utility class for security-related operations, particularly for device authorization checks.
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final DeviceInfoRepository deviceInfoRepository;

    /**
     * Checks if a user is the owner of a specific device.
     * Used in @PreAuthorize annotations for device-specific operations.
     *
     * @param deviceId The ID of the device to check
     * @param username The username (userId) of the user to verify
     * @return true if the user owns the device, false otherwise
     */
    public boolean isDeviceOwner(String deviceId, String username) {
        return deviceInfoRepository.findById(deviceId)
            .map(device -> device.getUserId().equals(username))
            .orElse(false);
    }
}
