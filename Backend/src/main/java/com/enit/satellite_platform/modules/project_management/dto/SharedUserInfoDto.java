package com.enit.satellite_platform.modules.project_management.dto;

import com.enit.satellite_platform.modules.project_management.entities.PermissionLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedUserInfoDto {
    private String userId;
    private String userName;
    private String userEmail;
    private PermissionLevel permissionLevel;
}
