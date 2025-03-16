package com.enit.satellite_platform.resources_management.dto;

import lombok.Data;
import java.util.Map;

import com.enit.satellite_platform.resources_management.config.ValidServiceType;

import jakarta.validation.constraints.NotBlank;

@Data
public class GeeRequest {

    @NotBlank(message = "serviceType is required")
    @ValidServiceType
    private String serviceType;
    private Map<String, Object> parameters;
    
    public GeeRequest(String serviceType, Map<String, Object> parameters) {
        this.serviceType = serviceType;
        this.parameters = parameters;
    }
    public GeeRequest() {
    }
}