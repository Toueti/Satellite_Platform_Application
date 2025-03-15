package com.enit.satellite_platform.resources_management.dto;

import lombok.Data;
import lombok.NonNull;

@Data
public class RetrieveImageRequest {
    public RetrieveImageRequest() {}

    @NonNull
    private String fileId;
}
