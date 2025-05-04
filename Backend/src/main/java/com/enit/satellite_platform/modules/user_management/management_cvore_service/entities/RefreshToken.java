package com.enit.satellite_platform.modules.user_management.management_cvore_service.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.security.device.DeviceInfo;
import lombok.Data;
import java.util.Date;

/**
 * Entity representing a refresh token used for JWT token renewal.
 * Links to device information for enhanced security tracking.
 */
@Data
@Document(collection = "refresh_tokens")
public class RefreshToken {
    @Id
    private String id;
    
    private String token;
    
    private String userId;
    
    @DBRef
    private DeviceInfo device;
    
    private Date expiryDate;
    
    private boolean isRevoked;
    
    private Date createdAt;
    
    // We keep these fields for quick access and historical tracking,
    // even though they're also stored in DeviceInfo
    private String ipAddress;
    private String userAgent;
    
    /**
     * Checks if the refresh token has expired.
     */
    public boolean isExpired() {
        return expiryDate != null && expiryDate.before(new Date());
    }
    
    /**
     * Checks if the refresh token is valid (not expired and not revoked).
     */
    public boolean isValid() {
        return !isExpired() && !isRevoked;
    }
    
    /**
     * Checks if the token can be used from the given device.
     */
    public boolean isValidForDevice(DeviceInfo requestDevice) {
        return device != null && 
               device.getId().equals(requestDevice.getId()) &&
               device.isApproved();
    }
}
