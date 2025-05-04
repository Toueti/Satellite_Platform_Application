package com.enit.satellite_platform.modules.user_management.management_cvore_service.security.device;

import com.enit.satellite_platform.modules.activity.service.ActivityLogService;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.security.device.dto.DeviceInfoDTO;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.security.device.exceptions.DeviceLimitExceededException;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.security.device.exceptions.DeviceNotFoundException;
import com.enit.satellite_platform.shared.utils.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing device-related operations.
 */
@Service
@RequiredArgsConstructor
public class DeviceManagementService {
    private static final Logger logger = LoggerFactory.getLogger(DeviceManagementService.class);
    private static final int MAX_DEVICES_PER_USER = 5;
    private static final int INACTIVE_DAYS_THRESHOLD = 30;

    private final DeviceInfoRepository deviceInfoRepository;
    private final DeviceIdentityProvider deviceIdentityProvider;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;

    /**
     * Registers or updates a device for a user during authentication.
     */
    @Transactional
    public DeviceInfo registerDevice(String userId, HttpServletRequest request) {
        String deviceIdentifier = deviceIdentityProvider.getDeviceIdentifier(request);
        DeviceMetadata metadata = deviceIdentityProvider.getDeviceMetadata(request);

        Optional<DeviceInfo> existingDevice = deviceInfoRepository
            .findByDeviceIdentifierAndUserId(deviceIdentifier, userId);

        if (existingDevice.isPresent()) {
            DeviceInfo device = existingDevice.get();
            device.updateFromMetadata(metadata);
            return deviceInfoRepository.save(device);
        }

        // Check if user has reached device limit
        long activeDevices = deviceInfoRepository.countActiveDevicesByUserId(
            userId, LocalDateTime.now().minusDays(INACTIVE_DAYS_THRESHOLD));
            
        if (activeDevices >= MAX_DEVICES_PER_USER) {
            logger.warn("User {} has reached maximum device limit", userId);
            throw new DeviceLimitExceededException("Maximum number of devices reached");
        }

        // Create new device
        DeviceInfo newDevice = new DeviceInfo(userId, metadata, deviceIdentifier);
        DeviceInfo savedDevice = deviceInfoRepository.save(newDevice);

        // Log new device registration
        activityLogService.logActivity(userId, "USER_ACTION", "DEVICE_REGISTERED",
            String.format("New device registered: %s (%s)", metadata.getDeviceType(), metadata.getBrowser()));

        return savedDevice;
    }

    /**
     * Gets all devices for a user, marking the current device.
     */
    public List<DeviceInfoDTO> getUserDevices(String userId, String currentDeviceId) {
        List<DeviceInfo> devices = deviceInfoRepository.findByUserId(userId);
        return devices.stream()
            .map(device -> DeviceInfoDTO.fromEntity(device, currentDeviceId))
            .collect(Collectors.toList());
    }

    /**
     * Gets active devices for a user (used within last 30 days).
     */
    public List<DeviceInfoDTO> getActiveDevices(String userId, String currentDeviceId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(INACTIVE_DAYS_THRESHOLD);
        List<DeviceInfo> devices = deviceInfoRepository.findActiveDevicesByUserId(userId, thirtyDaysAgo);
        return devices.stream()
            .map(device -> DeviceInfoDTO.fromEntity(device, currentDeviceId))
            .collect(Collectors.toList());
    }

    /**
     * Approves a device for continued use.
     */
    @Transactional
    public void approveDevice(String deviceId, String approvedByUserId) {
        DeviceInfo device = deviceInfoRepository.findById(deviceId)
            .orElseThrow(() -> new DeviceNotFoundException("Device not found: " + deviceId));

        device.approve(approvedByUserId);
        deviceInfoRepository.save(device);

        // Log approval
        activityLogService.logActivity(approvedByUserId, "ADMIN_ACTION", "DEVICE_APPROVED",
            String.format("Device approved: %s for user %s", deviceId, device.getUserId()));

        // Notify user
        notificationService.sendAlert("Device Approved",
            String.format("Your device (%s - %s) has been approved", 
                device.getDeviceType(), device.getBrowser()));
    }

    /**
     * Revokes a device's access.
     */
    @Transactional
    public void revokeDevice(String deviceId, String revokedByUserId) {
        DeviceInfo device = deviceInfoRepository.findById(deviceId)
            .orElseThrow(() -> new DeviceNotFoundException("Device not found: " + deviceId));

        device.revoke();
        deviceInfoRepository.save(device);

        // Log revocation
        activityLogService.logActivity(revokedByUserId, "ADMIN_ACTION", "DEVICE_REVOKED",
            String.format("Device revoked: %s for user %s", deviceId, device.getUserId()));

        // Notify user
        notificationService.sendAlert("Device Access Revoked",
            String.format("Access has been revoked for your device (%s - %s)", 
                device.getDeviceType(), device.getBrowser()));
    }

    /**
     * Removes inactive devices older than the threshold.
     */
    @Transactional
    public void cleanupInactiveDevices() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(INACTIVE_DAYS_THRESHOLD);
        List<DeviceInfo> inactiveDevices = deviceInfoRepository.findInactiveDevices(cutoffDate);

        if (!inactiveDevices.isEmpty()) {
            logger.info("Cleaning up {} inactive devices", inactiveDevices.size());
            deviceInfoRepository.deleteAll(inactiveDevices);
        }
    }

    /**
     * Checks if a device is approved for use.
     */
    public boolean isDeviceApproved(String userId, String deviceIdentifier) {
        return deviceInfoRepository
            .findByDeviceIdentifierAndUserId(deviceIdentifier, userId)
            .map(DeviceInfo::isApproved)
            .orElse(false);
    }
}
