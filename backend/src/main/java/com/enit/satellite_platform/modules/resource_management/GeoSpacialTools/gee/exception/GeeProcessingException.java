package com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.gee.exception;

import com.enit.satellite_platform.modules.resource_management.dto.ProcessingResponse;

public class GeeProcessingException extends RuntimeException {
    private ProcessingResponse geeResponse;
    public GeeProcessingException(String message) {
        super(message);
    }
     public GeeProcessingException(ProcessingResponse geeResponse) {

        super(geeResponse.getMessage() != null ? geeResponse.getMessage() : "GEE processing error");
        this.geeResponse = geeResponse;
    }

    public GeeProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
     public ProcessingResponse getGeeResponse() {
        return geeResponse;
    }
}
