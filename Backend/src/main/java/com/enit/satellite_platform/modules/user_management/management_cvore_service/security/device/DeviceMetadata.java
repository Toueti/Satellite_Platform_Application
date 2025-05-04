package com.enit.satellite_platform.modules.user_management.management_cvore_service.security.device;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data class containing device metadata information.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceMetadata {
    private String userAgent;
    private String ipAddress;
    private String deviceType;  // MOBILE, DESKTOP, TABLET, etc.
    private String operatingSystem;
    private String browser;
    private LocalDateTime lastUsedAt;
    
    // Constructor for basic metadata
    public DeviceMetadata(String userAgent, String ipAddress, LocalDateTime lastUsedAt) {
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
        this.lastUsedAt = lastUsedAt;
        
        // Parse user agent to set device type, OS, and browser
        parseUserAgent(userAgent);
    }
    
    private void parseUserAgent(String userAgent) {
        // Simple user agent parsing logic
        userAgent = userAgent.toLowerCase();
        
        // Determine device type
        if (userAgent.contains("mobile") || userAgent.contains("android") || userAgent.contains("iphone")) {
            this.deviceType = "MOBILE";
        } else if (userAgent.contains("tablet") || userAgent.contains("ipad")) {
            this.deviceType = "TABLET";
        } else {
            this.deviceType = "DESKTOP";
        }
        
        // Determine OS
        if (userAgent.contains("windows")) {
            this.operatingSystem = "Windows";
        } else if (userAgent.contains("mac")) {
            this.operatingSystem = "MacOS";
        } else if (userAgent.contains("linux")) {
            this.operatingSystem = "Linux";
        } else if (userAgent.contains("android")) {
            this.operatingSystem = "Android";
        } else if (userAgent.contains("ios") || userAgent.contains("iphone") || userAgent.contains("ipad")) {
            this.operatingSystem = "iOS";
        } else {
            this.operatingSystem = "Unknown";
        }
        
        // Determine browser
        if (userAgent.contains("chrome") && !userAgent.contains("edg")) {
            this.browser = "Chrome";
        } else if (userAgent.contains("firefox")) {
            this.browser = "Firefox";
        } else if (userAgent.contains("safari") && !userAgent.contains("chrome")) {
            this.browser = "Safari";
        } else if (userAgent.contains("edg")) {
            this.browser = "Edge";
        } else {
            this.browser = "Unknown";
        }
    }
}
