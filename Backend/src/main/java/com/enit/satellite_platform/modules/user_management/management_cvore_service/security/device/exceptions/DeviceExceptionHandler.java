package com.enit.satellite_platform.modules.user_management.management_cvore_service.security.device.exceptions;

import com.enit.satellite_platform.shared.dto.GenericResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Exception handler for device-related exceptions.
 */
@ControllerAdvice
public class DeviceExceptionHandler {

    @ExceptionHandler(DeviceNotFoundException.class)
    public ResponseEntity<GenericResponse<Void>> handleDeviceNotFound(DeviceNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new GenericResponse<>("ERROR", ex.getMessage()));
    }

    @ExceptionHandler(DeviceLimitExceededException.class)
    public ResponseEntity<GenericResponse<Void>> handleDeviceLimitExceeded(DeviceLimitExceededException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new GenericResponse<>("ERROR", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<GenericResponse<Void>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new GenericResponse<>("ERROR", "Device operation failed: " + ex.getMessage()));
    }
}
