package com.enit.satellite_platform.modules.user_management.normal_user_service.controllers;

import lombok.RequiredArgsConstructor;

import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.enit.satellite_platform.exceptions.DuplicationException;
import com.enit.satellite_platform.modules.user_management.admin_privileges.services.AdminServices;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.User;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.exceptions.InvalidCredentialsException;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.exceptions.InvalidTokenException;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.security.Jwt.JwtUtil;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.services.RefreshTokenService;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.RefreshToken;
import com.enit.satellite_platform.modules.user_management.normal_user_service.dtos.LoginRequest;
import com.enit.satellite_platform.modules.user_management.normal_user_service.dtos.RefreshTokenRequest; // Add RefreshTokenRequest
import com.enit.satellite_platform.modules.user_management.normal_user_service.dtos.ResetPasswordRequest;
import com.enit.satellite_platform.modules.user_management.normal_user_service.dtos.TokenResponse; // Add TokenResponse
import com.enit.satellite_platform.modules.user_management.normal_user_service.dtos.SignUpRequest;
import com.enit.satellite_platform.modules.user_management.normal_user_service.dtos.UserUpdateRequest;
import com.enit.satellite_platform.modules.user_management.normal_user_service.services.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest; // Add HttpServletRequest
import jakarta.validation.Valid; // Add Valid annotation

import org.springframework.security.core.userdetails.UsernameNotFoundException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor // Lombok handles constructor injection
public class AuthController {

    private final UserService authService;
    private final AdminServices adminServices;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService; // Add RefreshTokenService

    @Operation(summary = "Authenticate a user and return a JWT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User authenticated successfully, returns JWT"),
            @ApiResponse(responseCode = "400", description = "Invalid credentials")
    })
    @PostMapping("/auth/signin")
    public ResponseEntity<TokenResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) { // Add request, change return type
        try {
            TokenResponse tokenResponse = authService.accessUserAcount(loginRequest, request); // Pass request
            return ResponseEntity.ok(tokenResponse);
        } catch (InvalidCredentialsException e) {
            // Return TokenResponse with null tokens on error
            return ResponseEntity.badRequest().body(new TokenResponse(null, null, e.getMessage(), 0, null, null, null, null));
        }
    }

    @Operation(summary = "Register a new user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "User already exists or other error")
    })
    @PostMapping("/auth/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignUpRequest signUpRequest) {
        try {
            authService.addUser(signUpRequest);
            return ResponseEntity.ok("User registered successfully.");
        } catch (DuplicationException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("An error occurred during registration: " + e.getMessage());
        }
    }

    @Operation(summary = "Delete a user by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User deleted successfully"),
            @ApiResponse(responseCode = "400", description = "User not found or other error"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated")
    })
    @DeleteMapping("/thematician/signout/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        try {
            authService.deleteUser(new ObjectId(id));
            return ResponseEntity.ok("User deleted successfully");
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("An error occurred during user deletion: " + e.getMessage());
        }
    }

    @Operation(summary = "Update a user by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully, returns JWT"),
            @ApiResponse(responseCode = "400", description = "User not found, invalid credentials, or security error"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated")
    })
    @PutMapping("/account/update/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TokenResponse> updateUser(@PathVariable String id, @Valid @RequestBody UserUpdateRequest updatedUser) { // Change return type
        try {
            TokenResponse tokenResponse = authService.updateUser(new ObjectId(id), updatedUser);
            // Check if tokens were actually re-issued (they are null in the current implementation)
            if (tokenResponse.getAccessToken() == null) {
                // Return a specific response indicating success but no new tokens
                return ResponseEntity.ok(new TokenResponse(null, null, "User updated successfully. Please re-authenticate if needed.", 0, null, tokenResponse.getUsername(), tokenResponse.getEmail(), null));
            }
            return ResponseEntity.ok(tokenResponse);
        } catch (UsernameNotFoundException | InvalidCredentialsException | SecurityException e) {
            // Return TokenResponse with null tokens on error
            return ResponseEntity.badRequest().body(new TokenResponse(null, null, e.getMessage(), 0, null, null, null, null));
        }
    }

    @Operation(summary = "Get a user by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/thematician/account/{id}")
    public ResponseEntity<?> getUserById(@PathVariable String id) {
        try {
            User user = adminServices.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error retrieving user: " + e.getMessage());
        }
    }

    @Operation(summary = "Initiate password reset request")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset link sent"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody String email) {
        try {
            authService.resetPassword(email);
            return ResponseEntity.ok("Password reset link sent to " + email);
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing password reset: " + e.getMessage());
        }
    }

    @Operation(summary = "Reset password with token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest resetPasswordRequest) {
        try {
            authService.updatePasswordWithToken(resetPasswordRequest.getToken(), resetPasswordRequest.getNewPassword());
            return ResponseEntity.ok("Password updated successfully");
        } catch (InvalidTokenException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating password: " + e.getMessage());
        }
    }

    @Operation(summary = "Refresh JWT access token using a refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tokens refreshed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired refresh token")
    })
    @PostMapping("/auth/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest, HttpServletRequest request) {
        try {
            // First find the refresh token
            RefreshToken storedRefreshToken = refreshTokenService.findByToken(refreshTokenRequest.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));
            
            // Get the user from the stored token
            User user = authService.findById(storedRefreshToken.getUserId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            
            // Now pass both the refresh token and user to generate new tokens
            TokenResponse tokenResponse = jwtUtil.refreshToken(refreshTokenRequest.getRefreshToken(), request, user);
            return ResponseEntity.ok(tokenResponse);
        } catch (InvalidTokenException e) {
            return ResponseEntity.badRequest().body(new TokenResponse(null, null, e.getMessage(), 0, null, null, null, null));
        }
    }
}
