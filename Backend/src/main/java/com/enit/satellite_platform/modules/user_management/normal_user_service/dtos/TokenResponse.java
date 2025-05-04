package com.enit.satellite_platform.modules.user_management.normal_user_service.dtos;

import com.enit.satellite_platform.modules.user_management.management_cvore_service.security.device.DeviceInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private List<String> roles;
    private String username;
    private String email;
    private DeviceInfo deviceInfo;
}
