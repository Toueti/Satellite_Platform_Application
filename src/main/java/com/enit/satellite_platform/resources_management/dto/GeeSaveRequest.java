package com.enit.satellite_platform.resources_management.dto;

import lombok.Data;

@Data
public class GeeSaveRequest {
    
    public String imageId;
    public String data;
    public String date;
    private String type;
}
