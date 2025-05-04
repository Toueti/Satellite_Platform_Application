package com.enit.satellite_platform.modules.user_management.management_cvore_service.security.device.exceptions;

/**
 * Exception thrown when attempting to access a device that doesn't exist.
 */
public class DeviceNotFoundException extends RuntimeException {
    public DeviceNotFoundException(String message) {
        super(message);
    }
}
