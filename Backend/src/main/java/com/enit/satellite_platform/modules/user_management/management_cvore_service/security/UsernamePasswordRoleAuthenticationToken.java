package com.enit.satellite_platform.modules.user_management.management_cvore_service.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class UsernamePasswordRoleAuthenticationToken extends UsernamePasswordAuthenticationToken {
    private String role;
    
    // Constructor for an unauthenticated token (credentials provided)
    public UsernamePasswordRoleAuthenticationToken(Object principal, Object credentials, String role) {
        super(principal, credentials);
        this.role = role;
    }
    
    // Constructor for an authenticated token (after verification)
    public UsernamePasswordRoleAuthenticationToken(Object principal, Object credentials, String role, 
                                                     Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
        this.role = role;
    }
    
    /**
     * Gets the role of the user.
     *
     * @return The role of the user.
     */
    public String getRole() {
        return role;
    }
}
