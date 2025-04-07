package com.enit.satellite_platform.modules.user_management.user_service.dtos;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String token;
    private String newPassword;
}
