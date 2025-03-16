package com.enit.satellite_platform.resources_management.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class GeeResponse {
    @JsonProperty("status")
    private String status;

    @JsonProperty("date")
    private String date;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private Object data; 

    @JsonProperty("type")
    private String type;

    @JsonProperty("image_id")
    private String imageId;

    public GeeResponse(String status,String message) {
        this.status = status;
        this.message = message;
    }
    public GeeResponse(){}
}