package com.enit.satellite_platform.modules.user_management.normal_user_service.services;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException; // Keep for specific exceptions
import org.springframework.security.crypto.password.PasswordEncoder; // Re-add import
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.time.LocalDateTime;

import com.enit.satellite_platform.modules.activity.service.ActivityLogService;
import com.enit.satellite_platform.modules.project_management.entities.Project;
import com.enit.satellite_platform.modules.project_management.repositories.ProjectRepository;
import com.enit.satellite_platform.modules.resource_management.image_management.repositories.ImageRepository;
import com.enit.satellite_platform.modules.user_management.admin_privileges.repository.AdminSignupRequestRepository;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.AdminSignupRequest;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.Authority;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.User;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.exceptions.InvalidCredentialsException;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.exceptions.InvalidTokenException;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.exceptions.RoleNotFoundException;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.security.Jwt.JwtUtil;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.services.RoleService;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.services.UserManagementCoreService;
import com.enit.satellite_platform.modules.user_management.normal_user_service.dtos.LoginRequest;
import com.enit.satellite_platform.modules.user_management.normal_user_service.dtos.TokenResponse; // Add TokenResponse
import com.enit.satellite_platform.modules.user_management.normal_user_service.dtos.SignUpRequest;
import com.enit.satellite_platform.modules.user_management.normal_user_service.dtos.UserUpdateRequest;
import com.enit.satellite_platform.modules.user_management.normal_user_service.repositories.UserRepository;
import com.enit.satellite_platform.exceptions.DuplicationException;
import com.enit.satellite_platform.shared.utils.NotificationService; // Added import for NotificationService

import jakarta.servlet.http.HttpServletRequest; // Add HttpServletRequest
import java.util.HashSet;
import java.util.Set;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ImageRepository imageRepository;

    // Removed AuthorityRepository injection
    @Autowired
    private RoleService roleService; // Keep for checking ADMIN role in signup

    @Autowired
    private PasswordEncoder passwordEncoder; // Re-add injection

    @Autowired
    private UserManagementCoreService userManagementCoreService; // Inject Core Service

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired // Added import
    private AdminSignupRequestRepository adminSignupRequestRepository;

    @Autowired // Added injection for NotificationService
    private NotificationService notificationService;

    @Autowired // Added injection for ActivityLogService
    private ActivityLogService activityLogService;


    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    /**
     * Registers a new user with the provided sign-up request details.
     *
     * @param signUpRequest The sign-up request containing the user's registration
     *                      details such as username, email, password, and role.
     * @throws IllegalArgumentException   If a role other than 'THEMATICIAN' is
     *                                    provided during signup.
     * @throws RoleNotFoundException      If the default role 'THEMATICIAN' is not
     *                                    found in the database.
     * @throws DuplicationException If the email is already in use, indicating
     *                                    a duplicate registration attempt.
     */
    public void addUser(SignUpRequest signUpRequest) {
        boolean isAdminRequest = signUpRequest.getRoles() != null && signUpRequest.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role));

        // Check for duplicate email using Core Service
        try {
            userManagementCoreService.checkEmailDuplication(signUpRequest.getEmail(), null);
        } catch (DuplicationException e) {
             logger.warn("Duplicate email '{}' attempted during signup", signUpRequest.getEmail());
             throw new DuplicationException("Email '" + signUpRequest.getEmail() + "' is already in use! Please use a different email or login instead."); // Re-throw with specific message
        }

        // Check for pending admin request (this logic remains specific)
        if (isAdminRequest && adminSignupRequestRepository.existsByEmailAndStatus(signUpRequest.getEmail(), AdminSignupRequest.ApprovalStatus.PENDING)) {
             logger.warn("Pending admin signup request already exists for email: {}", signUpRequest.getEmail());
             throw new DuplicationException("An admin signup request for email '" + signUpRequest.getEmail() + "' is already pending approval.");
        }

        // Encode password using Core Service
        String encodedPassword = userManagementCoreService.encodePassword(signUpRequest.getPassword());

        // --- Handle Admin Signup Request ---
        if (isAdminRequest) {
            AdminSignupRequest adminRequest = new AdminSignupRequest(
                    signUpRequest.getUsername(),
                    signUpRequest.getEmail(),
                    encodedPassword);
            adminSignupRequestRepository.save(adminRequest);
            logger.info("Admin signup request submitted for email: {}. Awaiting approval.", signUpRequest.getEmail());

            // --- Send Notification to Admins ---
            try {
                Authority adminRole = roleService.findRoleByNameOrThrow("ROLE_ADMIN"); // Or just "ADMIN" depending on your naming
                List<User> admins = userRepository.findByAuthoritiesContains(adminRole);
                if (!admins.isEmpty()) {
                    String subject = "New Admin Signup Request";
                    String details = String.format("A new request for admin privileges has been submitted by user '%s' (Email: %s). Please review the request.",
                                                   signUpRequest.getUsername(), signUpRequest.getEmail());
                    for (User admin : admins) {
                        // Assuming sendAlert logs it, or potentially sends an email/other notification in the future
                        notificationService.sendAlert(subject, details + " [Admin: " + admin.getUsername() + "]");
                        logger.debug("Sent admin signup notification alert for request from {} to admin {}", signUpRequest.getEmail(), admin.getEmail());
                    }
                } else {
                    logger.warn("Admin signup request submitted, but no admin users found to notify.");
                }
            } catch (Exception e) {
                // Log error but don't fail the signup request process just because notification failed
                logger.error("Failed to send notification to admins about new signup request from {}", signUpRequest.getEmail(), e);
            }
            // --- End Notification ---

            return; // Stop further processing for admin requests
        }
        // --- End Admin Handling ---

        // --- Process Non-Admin Signup ---
        User user = new User();
        user.setName(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encodedPassword); // Use the already encoded password from core service

        Set<Authority> roles; // Initialize later
        List<String> requestedRoles = signUpRequest.getRoles();
        // Ensure roles are provided and not empty for non-admin signups
        if (requestedRoles == null || requestedRoles.isEmpty()) {
             throw new IllegalArgumentException("At least one role must be specified for signup.");
        }

        for (String roleInput : requestedRoles) {
            // Double-check to prevent ADMIN role assignment here (should be caught above, but belt-and-suspenders)
            if ("ROLE_ADMIN".equalsIgnoreCase(roleInput) || "ADMIN".equalsIgnoreCase(roleInput)) {
                  logger.error("Attempted to assign ADMIN role directly in non-admin flow for email: {}", signUpRequest.getEmail());
                  throw new IllegalArgumentException("Internal error: ADMIN role assignment attempted outside of approval process.");
             }
             // Role resolution will happen via core service below
        }
        // Resolve roles using Core Service (convert List to Set first)
        roles = userManagementCoreService.resolveRoles(new HashSet<>(requestedRoles));
        user.setAuthorities(roles);

        // Save the non-admin user using Core Service
        userManagementCoreService.saveUser(user); // Capture the saved user
        logger.info("User signed up successfully with email: {}", signUpRequest.getEmail());

    }

    /**
     * Authenticates a user based on the provided login credentials and returns a
     * JWT response.
     *
     * @param loginRequest The login request containing the user's username (email)
     *                     and password.
     * @return A JwtResponse containing the JWT token if authentication is
     *         successful.
     * @throws InvalidCredentialsException            If the provided username or
     *                                                password is incorrect.
     * @throws DisabledException                      If the user account is
     *                                                disabled.
     * @throws LockedException                        If the user account is locked.
     * @throws BadCredentialsException                If the provided username or
     *                                                password is incorrect.
     * @throws InternalAuthenticationServiceException If there is an internal error
     *                                                during authentication.
     */
    public TokenResponse accessUserAcount(LoginRequest loginRequest, HttpServletRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            // String token = jwtUtil.generateToken(userDetails); // Old method call
            TokenResponse tokenResponse = jwtUtil.generateTokens(userDetails, request); // New method call

            logger.info("User signed in successfully with email: {}", userDetails.getUsername());

            // Log successful login activity
            // Need to fetch the User object to get the ID
            User loggedInUser = userRepository.findByEmail(userDetails.getUsername())
                .orElse(null); // Should exist if authentication succeeded
            if (loggedInUser != null) {
                 activityLogService.logActivity(loggedInUser, "USER_LOGIN", "Successful login");
            } else {
                 // Log with username only if user object fetch fails unexpectedly
                 activityLogService.logActivity(null, userDetails.getUsername(), "USER_LOGIN", "Successful login (User object lookup failed)");
                 logger.error("Could not find User object for successfully authenticated user: {}", userDetails.getUsername());
            }

            return tokenResponse;
        } catch (BadCredentialsException e) {
            logger.warn("Invalid login attempt for username: {}", loginRequest.getUsername());
            // Send Alert
            notificationService.sendAlert("Failed Login Attempt", "Invalid credentials provided for username: " + loginRequest.getUsername());
            throw new InvalidCredentialsException("Invalid username or password");
        }
    }

    /**
     * Updates the password for a user with the given ID, provided the old password
     * matches.
     * This method first finds the user with the given ID and checks if the old
     * password matches.
     * If the old password is valid, the password is updated with the new password.
     * If the new password is in use by another user, a DuplicationException
     * is thrown.
     *
     * @param id          The ID of the user to update.
     * @param oldPassword The old password to check against.
     * @param newPassword The new password to set.
     * @throws UsernameNotFoundException   If the user with the given ID is not
     *                                     found.
     * @throws InvalidCredentialsException If the old password provided is
     *                                     incorrect.
     * @throws DuplicationException  If the email is already in use (should not happen if only password changes).
     */
    public void updatePassword(ObjectId id, String oldPassword, String newPassword) {
        // Find user using Core Service
        User user = userManagementCoreService.findUserByIdOrThrow(id);

        // Check old password using PasswordEncoder directly (as core service doesn't expose match)
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
             throw new InvalidCredentialsException("Invalid old password");
        }
        // Encode new password using Core Service
        user.setPassword(userManagementCoreService.encodePassword(newPassword));
        // Save user using Core Service
        userManagementCoreService.saveUser(user);
        logger.info("Password updated for user with id: {}", id);
        // Note: Duplication check on save is less likely here if only password changes,
        // but the core save method handles it if other fields were somehow modified.
    }

    /**
     * Updates the user details for the user with the given ID, provided the old
     * password matches.
     * The user's details are compared against the existing user details, and the
     * update is only
     * allowed if the email is not already in use by another user.
     * The user performing the update must be the same user being updated
     * (determined by the JWT
     * token in the request).
     *
     * @param userId      The ID of the user to update.
     * @param updatedUser The updated user details.
     * @return A JwtResponse containing the new JWT token.
     * @throws UsernameNotFoundException   If the user with the given ID is not
     *                                     found.
     * @throws InvalidCredentialsException If the old password provided is
     *                                     incorrect.
     * @throws DuplicationException  If the email is already in use by another
     *                                     user.
     * @throws SecurityException           If the update is attempted by a user
     *                                     other than the one being updated.
     */
    public TokenResponse updateUser(ObjectId userId, UserUpdateRequest updatedUser) { // Change return type to TokenResponse
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = auth.getName(); // Email from JWT token

        // Find user using Core Service
        User user = userManagementCoreService.findUserByIdOrThrow(userId);

        if (!user.getEmail().equals(currentUserEmail)) {
            throw new SecurityException("You can only update your own account");
        }

        // Check old password directly
        if (updatedUser.getOldPassword() != null) {
            if (!passwordEncoder.matches(updatedUser.getOldPassword(), user.getPassword())) {
                throw new InvalidCredentialsException("Invalid old password");
            }
        } else {
            throw new InvalidCredentialsException("Old password is required to update user details");
        }

        // Check for potential duplicates before updating
        userManagementCoreService.checkUserDuplication(updatedUser.getUsername(), updatedUser.getEmail(), userId);

        // Update fields
        user.setName(updatedUser.getUsername());
        user.setEmail(updatedUser.getEmail());
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
             // Encode new password using Core Service if provided
            user.setPassword(userManagementCoreService.encodePassword(updatedUser.getPassword()));
        }

        // Save user using Core Service
        User savedUser = userManagementCoreService.saveUser(user);
        logger.info("User updated: {}", userId);

        // Generate token based on the potentially updated user details
        // String newToken = jwtUtil.generateToken(savedUser); // Old method call
        // Need HttpServletRequest to generate new tokens, which isn't available here.
        // For now, let's return null or throw an exception, as updating user details shouldn't typically re-issue tokens immediately.
        // A better approach might be to invalidate old tokens and require re-login after sensitive updates.
        // Let's return null for now and revisit if token re-issuance is strictly required here.
        logger.warn("User details updated for {}, but tokens were not re-issued in this flow.", userId);
        // Returning null might cause issues if the controller expects a response.
        // Let's modify the controller later to handle this. For now, return a response with null tokens.
        return new TokenResponse(null, null, null, 0, null, savedUser.getUsername(), savedUser.getEmail(), null);
        // TODO: Revisit token re-issuance strategy after user update.
    }

    /**
     * Deletes a user and all their associated projects by their ID.
     *
     * <p>
     * This method performs the following actions:
     * </p>
     * <ul>
     * <li>Retrieves the user by their ID. If the user is not found, a
     * {@link UsernameNotFoundException} is thrown.</li>
     * <li>Finds all projects owned by this user. If any projects are found, they
     * are
     * deleted, along with all associated images.</li>
     * <li>Deletes the user from the repository.</li>
     * </ul>
     *
     * <p>
     * Transactional behavior ensures that all operations are atomic.
     * </p>
     *
     * @param userId The ID of the user to delete.
     * @throws UsernameNotFoundException If the user with the given ID is not found.
     */

    @Transactional
    public void deleteUser(ObjectId userId) {
        List<Project> ownedProjects = projectRepository.findAllByOwnerId(userId); 
        if (!ownedProjects.isEmpty()) {
            logger.info("Deleting {} projects owned by user with id: {}", ownedProjects.size(), userId);
            for (Project project : ownedProjects) {
                imageRepository.deleteAllByProject_Id(new ObjectId(project.getId()));
            }
            projectRepository.deleteAll(ownedProjects);
        } else {
            logger.info("No projects found for user with id: {}", userId);
        }

        // Delete the user using Core Service
        userManagementCoreService.deleteUserById(userId);
        logger.info("User deleted with id: {}", userId);
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param id The ID of the user to retrieve.
     * @return The user with the given ID, or a {@link UsernameNotFoundException} if
     *         no such user exists.
     */
    public User getUserById(ObjectId id) {
        // Find user using Core Service
        return userManagementCoreService.findUserByIdOrThrow(id);
    }
    
    /**
     * Retrieves a user by their ID string.
     *
     * @param id The ID string of the user to retrieve.
     * @return An Optional containing the user if found, or empty otherwise.
     */
    public java.util.Optional<User> findById(String id) {
        try {
            ObjectId objectId = new ObjectId(id);
            return userRepository.findById(objectId);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid ObjectId format: {}", id, e);
            return java.util.Optional.empty();
        }
    }

    public void resetPassword(String email) {
        // Find user using Core Service
        User user = userManagementCoreService.findUserByEmailOrThrow(email);

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(24)); // Token valid for 24 hours
        // Save user using Core Service
        userManagementCoreService.saveUser(user);

        // In a real application, you would send an email here.
        // For this example, we'll just log the reset link.
        String resetLink = "http://localhost:8080/reset-password?token=" + token;
        logger.info("Password reset requested for email: {}. Reset link: {}", email, resetLink);
    }

    public void updatePasswordWithToken(String token, String newPassword) {
        // Find user by token (this remains specific to UserService)
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid token"));

        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Token has expired");
        }

        // Encode new password using Core Service
        user.setPassword(userManagementCoreService.encodePassword(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        // Save user using Core Service
        userManagementCoreService.saveUser(user);
        logger.info("Password updated for user with email: {}", user.getEmail());
    }
}
