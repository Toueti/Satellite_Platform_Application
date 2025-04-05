package com.enit.satellite_platform.modules.user_management.admin_privileges.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfigUpdateRequest {

    @NotBlank(message = "Configuration key cannot be blank")
    private String key;

    private String value; // Value can be null or empty to potentially unset/clear a property
}
