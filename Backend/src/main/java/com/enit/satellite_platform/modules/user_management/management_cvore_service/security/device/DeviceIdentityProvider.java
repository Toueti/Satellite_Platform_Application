package com.enit.satellite_platform.modules.user_management.management_cvore_service.security.device;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Interface for providing device identification services.
 * This abstraction allows for future extensions of device identification methods.
 */
public interface DeviceIdentityProvider {
    /**
     * Get a basic device identifier based on available request information.
     * 
     * @param request The HTTP request containing device information
     * @return A string identifier for the device
     */
    String getDeviceIdentifier(HttpServletRequest request);
    
    /**
     * Get detailed metadata about the device.
     * 
     * @param request The HTTP request containing device information
     * @return DeviceMetadata object containing device details
     */
    DeviceMetadata getDeviceMetadata(HttpServletRequest request);
}
