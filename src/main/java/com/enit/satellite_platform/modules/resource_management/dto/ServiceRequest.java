package com.enit.satellite_platform.modules.resource_management.dto;

import lombok.Data;
import java.util.Map;

import com.enit.satellite_platform.modules.resource_management.config.ValidServiceType;

import jakarta.validation.constraints.NotBlank;


@Data
public class ServiceRequest {

    @NotBlank(message = "serviceType is required")
    @ValidServiceType
    private String serviceType;
    private Map<String, Object> parameters;
    
    public ServiceRequest(String serviceType, Map<String, Object> parameters) {
        this.serviceType = serviceType;
        this.parameters = parameters;
    }
    public ServiceRequest() {
    }
}