package com.enit.satellite_platform.user_management.dto;

import lombok.Data;

@Data
public class UserUpdateRequest {

    private String username;
    private String email;
    private String oldPassword;
    private String password;

}
