package com.enit.satellite_platform.modules.resource_management.image_management.dto;

import java.util.Map;

import org.springframework.web.multipart.MultipartFile; // Import MultipartFile

import com.enit.satellite_platform.modules.resource_management.image_management.entities.ProcessingStatus;
import com.enit.satellite_platform.modules.resource_management.image_management.entities.ProcessingType;
import com.fasterxml.jackson.annotation.JsonIgnore; // Import JsonIgnore

import lombok.Data;

@Data
public class resultsSaveRequest {

    public String imageId;
    public Map<String,Object> data;
    public String date;
    private ProcessingType type;
    private ProcessingStatus status; // Added status field

    @JsonIgnore // Ignore this field during JSON serialization/deserialization
    private MultipartFile file; // Field to hold the uploaded file
}
