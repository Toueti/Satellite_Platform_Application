package com.enit.satellite_platform.modules.resource_management.image_management.dto;

import lombok.Data;
import lombok.NonNull;

@Data
public class RetrieveImageRequest {
    public RetrieveImageRequest() {}

    @NonNull
    private String fileId;
}
