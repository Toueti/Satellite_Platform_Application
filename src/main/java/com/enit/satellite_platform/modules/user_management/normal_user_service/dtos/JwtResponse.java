package com.enit.satellite_platform.modules.user_management.normal_user_service.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String userId;
}
