package com.enit.satellite_platform.modules.user_management.management_cvore_service.security.Jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.enit.satellite_platform.config.JwtConfig;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.RefreshToken;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.User;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.services.RefreshTokenService;
import com.enit.satellite_platform.modules.user_management.normal_user_service.dtos.TokenResponse;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.exceptions.InvalidTokenException;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.security.device.DeviceInfo;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@SuppressWarnings("deprecation")
public class JwtUtil {

    private final JwtConfig jwtConfig;
    private final RefreshTokenService refreshTokenService;
    private final Key signingKey;

    public JwtUtil(JwtConfig jwtConfig, RefreshTokenService refreshTokenService) {
        this.jwtConfig = jwtConfig;
        this.refreshTokenService = refreshTokenService;
        this.signingKey = Keys.hmacShaKeyFor(jwtConfig.getSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate a JWT token containing the user's email, name, and roles.
     * @param userDetails the user details to extract the information from
     * @return the generated JWT token
     */
    public TokenResponse generateTokens(UserDetails userDetails, HttpServletRequest request) {
        User user = (User) userDetails;
        Date now = new Date();
        Date accessTokenExpiry = new Date(now.getTime() + jwtConfig.getExpirationTime());
        
        Map<String, Object> claims = Map.of(
            "name", user.getUsername(),
            "roles", user.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList())
        );

        String accessToken = Jwts.builder()
            .claims(claims)
            .subject(user.getEmail())
            .issuedAt(now)
            .expiration(accessTokenExpiry)
            .signWith(signingKey)
            .compact();
            
        // Create refresh token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId(), request);
        DeviceInfo deviceInfo = refreshToken.getDevice();
        
        List<String> roles = user.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());
            
        return new TokenResponse(
            accessToken,
            refreshToken.getToken(),
            "Bearer",
            (accessTokenExpiry.getTime() - now.getTime()) / 1000,
            roles,
            user.getName(),
            user.getEmail(),
            deviceInfo
        );
    }
    
    public TokenResponse refreshToken(String refreshToken, HttpServletRequest request, User user) {
        RefreshToken storedRefreshToken = refreshTokenService.findByToken(refreshToken)
            .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));
        
        if (!user.getId().toString().equals(storedRefreshToken.getUserId())) {
            throw new InvalidTokenException("Token does not belong to user");
        }
            
        // Revoke the used refresh token for security
        refreshTokenService.revokeToken(refreshToken);
        
        // Generate new tokens
        return generateTokens(user, request);
    }

    /**
     * Validates a JWT token by verifying its signature and checking if it has expired.
     * @param token the JWT token to validate
     * @return true if the token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            System.out.println("JWT Token expired: " + e.getMessage());
        } catch (JwtException e) {
            System.out.println("Invalid JWT Token: " + e.getMessage());
        }
        return false;
    }

    /**
     * Extracts the username (email) from the given JWT token.
     * @param token the JWT token
     * @return the username (email)
     */
    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Extracts the claims (username, roles, etc.) from the given JWT token.
     * @param token the JWT token
     * @return the claims
     */
    private Claims extractClaims(String token) {
        return Jwts.parser()
            .setSigningKey(signingKey)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
}
