package com.enit.satellite_platform.modules.resource_management.image_management.dto;

import lombok.Data;
import lombok.NonNull;

@Data
public class ListImagesRequest {
    public ListImagesRequest() {}

    @NonNull
    private String folderId;
    private int pageSize = 10;
    private String pageToken = "";
    private String orderBy = "name";
    private String queryFilter = "";
}
