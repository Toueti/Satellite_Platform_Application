package com.enit.satellite_platform.resources_management.exception;

import com.enit.satellite_platform.resources_management.dto.GeeResponse;

public class GeeProcessingException extends RuntimeException {
    private GeeResponse geeResponse;
    public GeeProcessingException(String message) {
        super(message);
    }
     public GeeProcessingException(GeeResponse geeResponse) {

        super(geeResponse.getMessage() != null ? geeResponse.getMessage() : "GEE processing error");
        this.geeResponse = geeResponse;
    }

    public GeeProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
     public GeeResponse getGeeResponse() {
        return geeResponse;
    }
}
