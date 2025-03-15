package com.enit.satellite_platform.resources_management.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class DeleteRequest {
    @JsonProperty("folder_id")
    private String folderId;
    private boolean recursive = false;

}
