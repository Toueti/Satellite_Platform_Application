package com.enit.satellite_platform.user_management.controller;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.enit.satellite_platform.user_management.dto.JwtResponse;
import com.enit.satellite_platform.user_management.dto.LoginRequest;
import com.enit.satellite_platform.user_management.dto.PasswordUpdateRequest;
import com.enit.satellite_platform.user_management.dto.SignUpRequest;
import com.enit.satellite_platform.user_management.dto.UserUpdateRequest;
import com.enit.satellite_platform.user_management.exception.InvalidCredentialsException;
import com.enit.satellite_platform.user_management.exception.UserAlreadyExistsException;
import com.enit.satellite_platform.user_management.model.User;
import com.enit.satellite_platform.user_management.service.AdminServices;
import com.enit.satellite_platform.user_management.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Controller for handling authentication and user management related requests.
 * Provides endpoints for user sign-in, sign-up, user deletion, user updates, and retrieval of user information.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

  private final UserService authService;

  private final AdminServices adminServices;

  /**
   * Authenticates a user and returns a JWT.
   *
   * @param loginRequest The login request containing the user's credentials.
   * @return A ResponseEntity containing the JWT response, or a bad request if authentication fails.
   * @throws InvalidCredentialsException If the provided credentials are invalid.
   */
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
      return ResponseEntity.badRequest().body(new JwtResponse(e.getMessage()));
    }
  }

  /**
   * Registers a new user.
   *
   * @param signUpRequest The sign-up request containing the user's registration details.
   * @return A ResponseEntity indicating successful registration, or a bad request if registration fails.
   * @throws UserAlreadyExistsException If a user with the given username or email already exists.
   */
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
    } catch (UserAlreadyExistsException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (Exception e) {
      return ResponseEntity.badRequest()
          .body("An error occurred during registration: " + e.getMessage());
    }
  }

  /**
   * Deletes a user by their ID.
   *
   * @param id The ID of the user to delete.
   * @return A ResponseEntity indicating successful deletion, or a bad request if deletion fails.
   * @throws UsernameNotFoundException If the user with the given ID is not found.
   */
  @Operation(summary = "Delete a user by ID")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "User deleted successfully"),
          @ApiResponse(responseCode = "400", description = "User not found or other error"),
          @ApiResponse(responseCode = "401", description = "User is not authenticated")
  })
  @DeleteMapping("/thematician/account/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<?> deleteUser(@PathVariable String id) {
    try {
      authService.deleteUser(new ObjectId(id));
      return ResponseEntity.ok("User deleted successfully");
    } catch (UsernameNotFoundException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (Exception e) {
      return ResponseEntity.badRequest()
          .body("An error occurred during user deletion: " + e.getMessage());
    }
  }

  /**
   * Updates a user's information by their ID.
   *
   * @param id The ID of the user to update.
   * @param updatedUser The updated user information.
   * @return A ResponseEntity containing the JWT response, or a bad request if the update fails.
   * @throws UsernameNotFoundException If the user with the given ID is not found.
   * @throws InvalidCredentialsException If the provided credentials are invalid.
   * @throws SecurityException If there is a security violation.
   */
  @Operation(summary = "Update a user by ID")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "User updated successfully, returns JWT"),
          @ApiResponse(responseCode = "400", description = "User not found, invalid credentials, or security error"),
          @ApiResponse(responseCode = "401", description = "User is not authenticated")
  })
  @PutMapping("/account/update/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<JwtResponse> updateUser(
      @PathVariable String id, @RequestBody UserUpdateRequest updatedUser) {
    try {
      JwtResponse jwtResponse = authService.updateUser(new ObjectId(id), updatedUser);
      return ResponseEntity.ok(jwtResponse);
    } catch (UsernameNotFoundException e) {
      return ResponseEntity.badRequest().body(new JwtResponse(e.getMessage()));
    } catch (InvalidCredentialsException e) {
      return ResponseEntity.badRequest().body(new JwtResponse(e.getMessage()));
    } catch (SecurityException e) {
      return ResponseEntity.badRequest().body(new JwtResponse(e.getMessage()));
    }
  }

  /**
   * Retrieves all users. This endpoint is restricted to administrators.
   *
   * @return A ResponseEntity containing the list of users, or a bad request if retrieval fails.
   */
  @Operation(summary = "Get all users (admin only)")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
          @ApiResponse(responseCode = "400", description = "Error retrieving users"),
          @ApiResponse(responseCode = "403", description = "User is not an admin")
  })
  @GetMapping("/admin/users")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<?> getAllUsers() {
    try {
      List<User> users = adminServices.getAllUsers();
      return ResponseEntity.ok(users);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("Error retrieving users: " + e.getMessage());
    }
  }

    /**
     * Retrieves a user by their ID.
     *
     * @param id The ID of the user to retrieve.
     * @return A ResponseEntity containing the user, or a bad request if retrieval fails.
     * @throws UsernameNotFoundException If the user with the given ID is not found.
     */
    @Operation(summary = "Get a user by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/admin/account/{id}")
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

    /**
     * Initiates a password reset for the user with the given email.
     *
     * @param email The email of the user requesting a password reset.
     * @return A ResponseEntity indicating successful initiation, or a bad request if the process fails.
     * @throws UsernameNotFoundException If the user with the given email is not found.
     */
    @Operation(summary = "reset user password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset link sent"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody String email) {
        try {
            authService.resetPassword(email);
            return ResponseEntity.ok("Password reset link sent to " + email);
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing password reset: " + e.getMessage());
        }
    }

    /**
     * Updates the password for a user.
     *
     * @param id The ID of the user updating their password.
     * @param passwordUpdateRequest The request containing the old and new passwords.
     * @return A ResponseEntity indicating successful password update, or a bad request if the update fails.
     * @throws UsernameNotFoundException If the user with the given ID is not found.
     * @throws InvalidCredentialsException If the old password provided is incorrect.
     */
    @Operation(summary = "update user password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/thematician/account/{id}/update-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updatePassword(
            @PathVariable String id, @RequestBody PasswordUpdateRequest passwordUpdateRequest) {
        try {
            authService.updatePassword(
                    new ObjectId(id), passwordUpdateRequest.getOldPassword(), passwordUpdateRequest.getNewPassword());
            return ResponseEntity.ok("Password updated successfully");
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (InvalidCredentialsException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Error updating password: " + e.getMessage());
        }
    }
}
