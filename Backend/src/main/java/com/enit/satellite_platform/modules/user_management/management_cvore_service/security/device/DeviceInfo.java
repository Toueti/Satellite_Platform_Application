package com.enit.satellite_platform.modules.user_management.management_cvore_service.security.device;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Entity class representing a device used to access the system.
 * Stores device information and tracks its usage and approval status.
 */
@Data
@Document(collection = "device_info")
public class DeviceInfo {
    @Id
    private String id;
    
    private String userId;
    
    private String deviceIdentifier;
    
    private String userAgent;
    private String ipAddress;
    private String deviceType;
    private String operatingSystem;
    private String browser;
    
    private boolean isApproved;
    private LocalDateTime lastUsedAt;
    private LocalDateTime firstSeenAt;
    private LocalDateTime approvedAt;
    
    private String lastKnownLocation; // Can be expanded to store more detailed location data
    private String approvedBy; // User ID of the admin who approved the device, if applicable
    
    public DeviceInfo() {
        this.firstSeenAt = LocalDateTime.now();
        this.lastUsedAt = this.firstSeenAt;
        this.isApproved = false; // Devices start as unapproved by default
    }
    
    public DeviceInfo(String userId, DeviceMetadata metadata, String deviceIdentifier) {
        this();
        this.userId = userId;
        this.deviceIdentifier = deviceIdentifier;
        this.userAgent = metadata.getUserAgent();
        this.ipAddress = metadata.getIpAddress();
        this.deviceType = metadata.getDeviceType();
        this.operatingSystem = metadata.getOperatingSystem();
        this.browser = metadata.getBrowser();
    }
    
    /**
     * Updates the device information with new metadata.
     */
    public void updateFromMetadata(DeviceMetadata metadata) {
        this.userAgent = metadata.getUserAgent();
        this.ipAddress = metadata.getIpAddress();
        this.deviceType = metadata.getDeviceType();
        this.operatingSystem = metadata.getOperatingSystem();
        this.browser = metadata.getBrowser();
        this.lastUsedAt = metadata.getLastUsedAt();
    }
    
    /**
     * Approves the device for use.
     * @param approvedBy User ID of the admin approving the device
     */
    public void approve(String approvedBy) {
        this.isApproved = true;
        this.approvedAt = LocalDateTime.now();
        this.approvedBy = approvedBy;
    }
    
    /**
     * Revokes the device's approval.
     */
    public void revoke() {
        this.isApproved = false;
        this.approvedAt = null;
        this.approvedBy = null;
    }
}
