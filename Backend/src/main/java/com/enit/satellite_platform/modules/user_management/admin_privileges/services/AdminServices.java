package com.enit.satellite_platform.modules.user_management.admin_privileges.services;

import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.enit.satellite_platform.exceptions.DuplicationException;
import com.enit.satellite_platform.modules.user_management.admin_privileges.repository.AdminSignupRequestRepository;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.AdminSignupRequest;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.Authority;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.User;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.exceptions.RoleNotFoundException;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.services.RoleService;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.services.UserManagementCoreService;
import com.enit.satellite_platform.modules.user_management.normal_user_service.repositories.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AdminServices {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleService roleService;


    @Autowired
    private UserManagementCoreService userManagementCoreService;


    @Autowired
    private AdminSignupRequestRepository adminSignupRequestRepository;

    @Autowired
    private AuditLogService auditLogService;


    private static final Logger logger = LoggerFactory.getLogger(AdminServices.class);

    private static final String ADMIN_ROLE_NAME = "ROLE_ADMIN";

    /**
     * Creates a new user with the specified username, email, password, and roles.
     *
     * @param username  The username for the new user.
     * @param email     The email address for the new user.
     * @param password  The password for the new user.
     * @param roleNames A set of role names to assign to the new user.
     * @return The created User object.
     * @throws DuplicationException If the username or email is already in use.
     * @throws RoleNotFoundException If any of the specified roles are not found.
     */
    public User createUser(String username, String email, String password, Set<String> roleNames) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty.");
        }
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty.");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty.");
        }

        // Check for duplicates using Core Service
        userManagementCoreService.checkUserDuplication(username, email, null);

        // Create the user
        User user = new User();
        user.setName(username);
        user.setEmail(email);
        // Encode password using Core Service
        user.setPassword(userManagementCoreService.encodePassword(password));

        // Resolve roles using Core Service
        Set<Authority> roles = userManagementCoreService.resolveRoles(roleNames);
        user.setAuthorities(roles);

        // Save user using Core Service
        User savedUser = userManagementCoreService.saveUser(user);
        // Audit Log
        auditLogService.logAuditEvent(getCurrentUsername(), "USER_CREATED", "Created user: " + savedUser.getUsername() + " (ID: " + savedUser.getId() + ") with roles: " + roleNames);
        return savedUser;
    }

    /**
     * Updates an existing user's information, including username, email, and roles.
     *
     * @param userId    The ID of the user to update.
     * @param username  The new username for the user.
     * @param email     The new email address for the user.
     * @param roleNames A set of role names to assign to the user.
     * @return The updated User object.
     * @throws UsernameNotFoundException If the user with the given ID is not found.
     * @throws DuplicationException If the new username or email is already taken by another user.
     * @throws RoleNotFoundException If any of the specified roles are not found.
     */
    public User updateUser(ObjectId userId, String username, String email, Set<String> roleNames) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null.");
        }
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty.");
        }
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty.");
        }

        // Find user using Core Service
        User user = userManagementCoreService.findUserByIdOrThrow(userId);

        // Capture old roles before updating
        user.getAuthorities().stream()
                                     .map(auth -> auth.getAuthority()) // Use lambda expression instead of method reference
                                     .collect(java.util.stream.Collectors.toSet());

        // Check for duplicates using Core Service (excluding current user)
        userManagementCoreService.checkUserDuplication(username, email, userId);

        // Update fields
        user.setName(username);
        user.setEmail(email);

        // Resolve roles using Core Service
        Set<Authority> roles = userManagementCoreService.resolveRoles(roleNames);
        user.setAuthorities(roles);

        // Save user using Core Service
        User savedUser = userManagementCoreService.saveUser(user);

        // Audit Log
        auditLogService.logAuditEvent(getCurrentUsername(), "USER_UPDATED", "Updated user: " + savedUser.getUsername() + " (ID: " + userId + ") - Roles set to: " + roleNames);

        return savedUser;
    }

    /**
     * Resets the password for the user with the given ID.
     *
     * @param userId      The ID of the user to reset the password for.
     * @param newPassword The new password for the user.
     * @throws UsernameNotFoundException If the user with the given ID is not found.
     */
    public void resetUserPassword(ObjectId userId, String newPassword) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null.");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            throw new IllegalArgumentException("New password cannot be null or empty.");
        }

        // Find user using Core Service
        User user = userManagementCoreService.findUserByIdOrThrow(userId);
        // Encode password using Core Service
        user.setPassword(userManagementCoreService.encodePassword(newPassword));
        // Save user using Core Service
        userManagementCoreService.saveUser(user);
        // Audit Log
        auditLogService.logAuditEvent(getCurrentUsername(), "PASSWORD_RESET", "Reset password for user: " + user.getUsername() + " (ID: " + userId + ")");
    }

    /**
     * Deletes a user by their ID.
     *
     * @param userId The ID of the user to delete.
     * @throws UsernameNotFoundException If the user with the given ID is not found.
     */
    public void deleteUser(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty.");
        }
        ObjectId objectId;
        try {
             objectId = new ObjectId(userId);
        } catch (IllegalArgumentException e) {
             throw new IllegalArgumentException("Invalid User ID format: " + userId);
        }

        // Find user to get username for logging before deletion
        User user = userManagementCoreService.findUserByIdOrThrow(objectId);
        String deletedUsername = user.getUsername(); // Capture username before deletion

        // Delete user using Core Service
        userManagementCoreService.deleteUserById(objectId);
        // Audit Log
        auditLogService.logAuditEvent(getCurrentUsername(), "USER_DELETED", "Deleted user: " + deletedUsername + " (ID: " + userId + ")");
    }

    /**
     * Retrieves a list of all users in the system.
     *
     * @return A list of User objects.
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param id The String representation of the user's ID.
     * @return The user with the given ID.
     * @throws UsernameNotFoundException If the user with the given ID is not found.
     */
    public User getUserById(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty.");
        }
        ObjectId objectId;
         try {
             objectId = new ObjectId(id);
        } catch (IllegalArgumentException e) {
             throw new IllegalArgumentException("Invalid User ID format: " + id);
        }
        // Find user using Core Service
        return userManagementCoreService.findUserByIdOrThrow(objectId);
    }

    /**
     * Locks a user account by disabling it.
     *
     * @param userId The ID of the user whose account is to be locked.
     * @throws UsernameNotFoundException If the user with the given ID is not found.
     */

    public void lockUnlockUserAccount(String userId, boolean lock) {
        // getUserById already uses the core service method due to previous refactoring
        User user = getUserById(userId);
        user.setEnabled(!lock); // setEnabled(true) means unlocked, setEnabled(false) means locked
        // Save user using Core Service
        userManagementCoreService.saveUser(user);
        // Audit Log
        String action = lock ? "ACCOUNT_LOCKED" : "ACCOUNT_UNLOCKED";
        auditLogService.logAuditEvent(getCurrentUsername(), action, "User account: " + user.getUsername() + " (ID: " + userId + ")");
    }

    // --- Admin Signup Request Management ---

    /**
     * Retrieves all pending admin signup requests.
     *
     * @return A list of AdminSignupRequest objects with PENDING status.
     */
    public List<AdminSignupRequest> getPendingAdminRequests() {
        return adminSignupRequestRepository.findByStatus(AdminSignupRequest.ApprovalStatus.PENDING);
    }

    /**
     * Approves a pending admin signup request.
     * Creates the user with the ADMIN role and updates the request status.
     *
     * @param requestId The ID of the AdminSignupRequest to approve.
     * @return The newly created User object.
     * @throws EntityNotFoundException If the request ID is not found or the request is not pending.
     * @throws DuplicationException If the email is already associated with an existing user.
     * @throws RoleNotFoundException If the ADMIN role is not found in the database.
     */
    @Transactional
    public User approveAdminRequest(String requestId) {
        AdminSignupRequest request = adminSignupRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Admin signup request not found with ID: " + requestId));

        if (request.getStatus() != AdminSignupRequest.ApprovalStatus.PENDING) {
            throw new IllegalStateException("Request is not in PENDING status. Current status: " + request.getStatus());
        }

        // Check if user already exists using Core Service
        if (userManagementCoreService.existsByEmail(request.getEmail())) {
            // Mark request as rejected due to duplication
            request.setStatus(AdminSignupRequest.ApprovalStatus.REJECTED);
            request.setDecisionTimestamp(LocalDateTime.now());
            // Optionally set approvedByUserId based on current admin context
            adminSignupRequestRepository.save(request);
            logger.warn("Admin request {} rejected automatically due to existing user with email {}", requestId, request.getEmail());
            throw new DuplicationException("User with email '" + request.getEmail() + "' already exists.");
        }

        // Find the ADMIN role using RoleService
        Authority adminRole = roleService.findRoleByNameOrThrow(ADMIN_ROLE_NAME);
        // RoleNotFoundException is handled within findRoleByNameOrThrow if role doesn't exist
        if (adminRole == null) { // Should not happen due to exception handling in service, but as safeguard
             logger.error("CRITICAL: {} role could not be retrieved via RoleService!", ADMIN_ROLE_NAME);
             // Manually throw or handle, though service should have thrown RoleNotFoundException
             throw new RoleNotFoundException(ADMIN_ROLE_NAME + " role not found. Cannot create admin user.");
        } // Removed extra });

        // Create the new admin user
        User newUser = new User();
        newUser.setName(request.getUsername());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(request.getEncodedPassword()); // Use the pre-encoded password
        newUser.setAuthorities(Set.of(adminRole));
        newUser.setEnabled(true); // Ensure user is enabled
        newUser.setLocked(false); // Ensure user is not locked

        // Save user using Core Service
        User savedUser = userManagementCoreService.saveUser(newUser);
        logger.info("Admin user created successfully for email: {}", savedUser.getEmail());

        // Update the request status
        request.setStatus(AdminSignupRequest.ApprovalStatus.APPROVED);
        request.setDecisionTimestamp(LocalDateTime.now());

        // Get current admin user ID for auditing (if possible)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserDetails) {
             User currentUser = userRepository.findByEmail(authentication.getName()).orElse(null);
             if (currentUser != null) {
                 request.setApprovedByUserId(currentUser.getId().toString());
             } else {
                  logger.warn("Could not determine approving admin user ID for request {}", requestId);
             }
        } else {
             logger.warn("Could not determine approving admin user context for request {}", requestId);
        }


        adminSignupRequestRepository.save(request);
        logger.info("Admin signup request {} approved.", requestId);

        // Audit Log for approval and user creation
        auditLogService.logAuditEvent(getCurrentUsername(), "ADMIN_REQUEST_APPROVED", "Approved request ID: " + requestId + " for email: " + request.getEmail());
        auditLogService.logAuditEvent(getCurrentUsername(), "ADMIN_USER_CREATED", "Created admin user: " + savedUser.getUsername() + " (ID: " + savedUser.getId() + ") from request ID: " + requestId);

        return savedUser;
    }

    /**
     * Rejects a pending admin signup request.
     * Updates the request status to REJECTED.
     *
     * @param requestId The ID of the AdminSignupRequest to reject.
     * @throws EntityNotFoundException If the request ID is not found or the request is not pending.
     */
    @Transactional
    public void rejectAdminRequest(String requestId) {
        AdminSignupRequest request = adminSignupRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Admin signup request not found with ID: " + requestId));

        if (request.getStatus() != AdminSignupRequest.ApprovalStatus.PENDING) {
            throw new IllegalStateException("Request is not in PENDING status. Current status: " + request.getStatus());
        }

        request.setStatus(AdminSignupRequest.ApprovalStatus.REJECTED);
        request.setDecisionTimestamp(LocalDateTime.now());

         // Get current admin user ID for auditing (if possible)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserDetails) {
             User currentUser = userRepository.findByEmail(authentication.getName()).orElse(null);
             if (currentUser != null) {
                 request.setApprovedByUserId(currentUser.getId().toString()); // Use same field for rejecter ID
             } else {
                  logger.warn("Could not determine rejecting admin user ID for request {}", requestId);
             }
        } else {
             logger.warn("Could not determine rejecting admin user context for request {}", requestId);
        }


        adminSignupRequestRepository.save(request);
        logger.info("Admin signup request {} rejected.", requestId);
        // Audit Log
        auditLogService.logAuditEvent(getCurrentUsername(), "ADMIN_REQUEST_REJECTED", "Rejected request ID: " + requestId + " for email: " + request.getEmail());
    }

    /**
     * Helper method to get the username of the currently authenticated user.
     * @return The username or "SYSTEM" if no authentication context is found.
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return "SYSTEM"; // Or handle as appropriate if action must be user-initiated
        }
        String username = authentication.getName();
        
        if (authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            username = userDetails.getUsername();
        }
        return username;
    }
}
