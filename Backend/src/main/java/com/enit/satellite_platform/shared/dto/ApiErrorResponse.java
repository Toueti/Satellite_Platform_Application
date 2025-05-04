package com.enit.satellite_platform.shared.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApiErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;

    public ApiErrorResponse(String error, String message) {
        this.timestamp = LocalDateTime.now();
        this.status = 400;
        this.error = error;
        this.message = message;
    }
}