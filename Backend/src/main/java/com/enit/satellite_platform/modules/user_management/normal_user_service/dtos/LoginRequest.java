package com.enit.satellite_platform.modules.user_management.normal_user_service.dtos;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
    private String role;
}
