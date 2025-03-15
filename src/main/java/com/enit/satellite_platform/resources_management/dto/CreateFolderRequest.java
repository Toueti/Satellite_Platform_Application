package com.enit.satellite_platform.resources_management.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class CreateFolderRequest {
    @JsonProperty("folder_name")
    private String folderName;
    @JsonProperty("parent_id")
    private String parentId;

}
