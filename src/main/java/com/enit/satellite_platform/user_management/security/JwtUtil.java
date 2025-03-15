package com.enit.satellite_platform.user_management.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import com.enit.satellite_platform.user_management.config.JwtConfig;
import com.enit.satellite_platform.user_management.model.User;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@SuppressWarnings("deprecation")
public class JwtUtil {

    private final JwtConfig jwtConfig;
    private final Key signingKey;

    public JwtUtil(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        this.signingKey = Keys.hmacShaKeyFor(jwtConfig.getSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate a JWT token containing the user's email, name, and roles.
     * @param userDetails the user details to extract the information from
     * @return the generated JWT token
     */
    public String generateToken(UserDetails userDetails) {
        User user = (User) userDetails;
        Map<String, Object> claims = Map.of(
            "email", user.getEmail(),
            "name", user.getUsername(),
            "roles", user.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList())
        );

        return Jwts.builder()
            .claims(claims)
            .subject(user.getEmail())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtConfig.getExpirationTime()))
            .signWith(signingKey)
            .compact();
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
