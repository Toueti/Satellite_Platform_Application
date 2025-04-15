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
import com.enit.satellite_platform.modules.user_management.normal_user_service.dtos.JwtResponse;
import com.enit.satellite_platform.modules.user_management.normal_user_service.dtos.LoginRequest;
import com.enit.satellite_platform.modules.user_management.normal_user_service.dtos.ResetPasswordRequest;
import com.enit.satellite_platform.modules.user_management.normal_user_service.dtos.SignUpRequest;
import com.enit.satellite_platform.modules.user_management.normal_user_service.dtos.UserUpdateRequest;
import com.enit.satellite_platform.modules.user_management.normal_user_service.services.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final UserService authService;
    private final AdminServices adminServices;

    @Operation(summary = "Authenticate a user and return a JWT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User authenticated successfully, returns JWT"),
            @ApiResponse(responseCode = "400", description = "Invalid credentials")
    })
    @PostMapping("/auth/signin")
    public ResponseEntity<JwtResponse> authenticateUser(@RequestBody LoginRequest loginRequest) {
        try {
            JwtResponse jwtResponse = authService.accessUserAcount(loginRequest);
            return ResponseEntity.ok(jwtResponse);
        } catch (InvalidCredentialsException e) {
            return ResponseEntity.badRequest().body(new JwtResponse(e.getMessage(), null));
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
    public ResponseEntity<JwtResponse> updateUser(@PathVariable String id, @RequestBody UserUpdateRequest updatedUser) {
        try {
            JwtResponse jwtResponse = authService.updateUser(new ObjectId(id), updatedUser);
            return ResponseEntity.ok(jwtResponse);
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.badRequest().body(new JwtResponse(e.getMessage(), null));
        } catch (InvalidCredentialsException e) {
            return ResponseEntity.badRequest().body(new JwtResponse(e.getMessage(), null));
        } catch (SecurityException e) {
            return ResponseEntity.badRequest().body(new JwtResponse(e.getMessage(), null));
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
}
