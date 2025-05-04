package com.enit.satellite_platform.modules.user_management.management_cvore_service.security.device.dto;

import com.enit.satellite_platform.modules.user_management.management_cvore_service.security.device.DeviceInfo;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for transferring device information to clients.
 */
@Data
public class DeviceInfoDTO {
    private String id;
    private String deviceType;
    private String operatingSystem;
    private String browser;
    private String lastKnownLocation;
    private LocalDateTime lastUsedAt;
    private LocalDateTime firstSeenAt;
    private boolean isCurrentDevice;
    private boolean isApproved;
    private LocalDateTime approvedAt;
    
    public static DeviceInfoDTO fromEntity(DeviceInfo deviceInfo, String currentDeviceId) {
        DeviceInfoDTO dto = new DeviceInfoDTO();
        dto.setId(deviceInfo.getId());
        dto.setDeviceType(deviceInfo.getDeviceType());
        dto.setOperatingSystem(deviceInfo.getOperatingSystem());
        dto.setBrowser(deviceInfo.getBrowser());
        dto.setLastKnownLocation(deviceInfo.getLastKnownLocation());
        dto.setLastUsedAt(deviceInfo.getLastUsedAt());
        dto.setFirstSeenAt(deviceInfo.getFirstSeenAt());
        dto.setApproved(deviceInfo.isApproved());
        dto.setApprovedAt(deviceInfo.getApprovedAt());
        
        // Set whether this is the current device
        dto.setCurrentDevice(deviceInfo.getDeviceIdentifier().equals(currentDeviceId));
        
        return dto;
    }
}
