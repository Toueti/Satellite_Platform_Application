package com.enit.satellite_platform.modules.user_management.management_cvore_service.security.device.exceptions;

/**
 * Exception thrown when a user attempts to register more devices than allowed.
 */
public class DeviceLimitExceededException extends RuntimeException {
    public DeviceLimitExceededException(String message) {
        super(message);
    }
}
