package com.enit.satellite_platform.modules.user_management.admin_privileges.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateRoleRequest {
    @NotBlank(message = "Role name cannot be blank")
    private String roleName;
}
