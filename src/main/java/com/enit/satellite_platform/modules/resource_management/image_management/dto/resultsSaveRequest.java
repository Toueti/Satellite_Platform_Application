package com.enit.satellite_platform.modules.resource_management.image_management.dto;

import java.util.Map;

import lombok.Data;

@Data
public class resultsSaveRequest {
    
    public String imageId;
    public Map<String,Object> data;
    public String date;
    private String type;
}
