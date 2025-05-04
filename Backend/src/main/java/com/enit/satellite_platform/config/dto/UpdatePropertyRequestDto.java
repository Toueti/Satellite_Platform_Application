package com.enit.satellite_platform.config.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdatePropertyRequestDto {
    @NotBlank(message = "Configuration key cannot be blank")
    private String key;

    // Value can be null. If null, the service might interpret this as "reset to default".
    private String value;
}
