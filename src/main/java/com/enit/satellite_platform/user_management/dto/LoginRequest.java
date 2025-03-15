package com.enit.satellite_platform.user_management.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
    private String role;
}
