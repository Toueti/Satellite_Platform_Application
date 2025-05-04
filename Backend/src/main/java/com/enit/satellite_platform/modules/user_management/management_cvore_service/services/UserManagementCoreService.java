package com.enit.satellite_platform.modules.user_management.management_cvore_service.services;

import com.enit.satellite_platform.exceptions.DuplicationException;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.Authority;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.User;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.exceptions.RoleNotFoundException;
import com.enit.satellite_platform.modules.user_management.normal_user_service.repositories.UserRepository;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Core service handling common user management operations like
 * finding users, checking duplicates, encoding passwords, resolving roles,
 * and saving users. Designed to be used by other services like
 * AdminServices and UserService to reduce redundancy.
 */
@Service
public class UserManagementCoreService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleService roleService;

    /**
     * Finds a user by their ObjectId or throws UsernameNotFoundException.
     *
     * @param userId The ObjectId of the user.
     * @return The found User.
     * @throws UsernameNotFoundException if the user is not found.
     */
    public User findUserByIdOrThrow(ObjectId userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null.");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
    }

     /**
     * Finds a user by their email or throws UsernameNotFoundException.
     *
     * @param email The email of the user.
     * @return The found User.
     * @throws UsernameNotFoundException if the user is not found.
     */
    public User findUserByEmailOrThrow(String email) {
         if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty.");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }


    /**
     * Checks for duplicate username or email, optionally excluding a specific user ID.
     *
     * @param username       The username to check.
     * @param email          The email to check.
     * @param existingUserId The ObjectId of the user being updated (null if creating a new user).
     * @throws DuplicationException if a duplicate is found.
     */
    public void checkUserDuplication(String username, String email, ObjectId existingUserId) {
        // Check username duplication
        if (userRepository.existsByName(username)) {
            if (existingUserId == null) {
                // Creating a new user, username exists -> duplicate
                throw new DuplicationException("Username '" + username + "' is already taken!");
            } else {
                // Updating an existing user, check if the existing username belongs to *this* user
                User existingUser = findUserByIdOrThrow(existingUserId); // Fetch the user being updated
                if (!existingUser.getUsername().equals(username)) {
                    // The username exists and it's not the user being updated -> duplicate
                    throw new DuplicationException("Username '" + username + "' is already taken!");
                }
                // If usernames match, it's okay (user is keeping their username)
            }
        }

        // Check email duplication (using the existing method which is correct)
        checkEmailDuplication(email, existingUserId);
    }

     /**
     * Checks if an email exists, optionally excluding a specific user ID.
     * (Simplified version for cases where only email uniqueness matters).
     *
     * @param email          The email to check.
     * @param existingUserId The ObjectId of the user being updated (null if creating a new user).
     * @throws DuplicationException if a duplicate email is found.
     */
    public void checkEmailDuplication(String email, ObjectId existingUserId) {
        Optional<User> userByEmail = userRepository.findByEmail(email);
        if (userByEmail.isPresent() && (existingUserId == null || !userByEmail.get().getId().equals(existingUserId))) {
            throw new DuplicationException("Email '" + email + "' is already in use!");
        }
    }


    /**
     * Encodes a raw password using the configured PasswordEncoder.
     *
     * @param rawPassword The plain text password.
     * @return The encoded password hash.
     */
    public String encodePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty.");
        }
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Resolves a set of role names into a set of Authority objects using RoleService.
     *
     * @param roleNames The set of role names (e.g., "ROLE_USER", "ADMIN").
     * @return A set of corresponding Authority objects.
     * @throws RoleNotFoundException if any role name is not found.
     */
    public Set<Authority> resolveRoles(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
             throw new IllegalArgumentException("Role names cannot be null or empty.");
            // Or return Collections.emptySet() depending on desired behavior
        }
        Set<Authority> roles = new HashSet<>();
        for (String roleName : roleNames) {
            Authority role = roleService.findRoleByNameOrThrow(roleName);
            roles.add(role);
        }
        return roles;
    }

    /**
     * Saves a User entity using the UserRepository.
     *
     * @param user The User object to save.
     * @return The saved User object, potentially with updated fields (like ID).
     */
    public User saveUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User object cannot be null.");
        }
        return userRepository.save(user);
    }

     /**
     * Deletes a User entity using the UserRepository.
     *
     * @param user The User object to delete.
     */
    public void deleteUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User object cannot be null.");
        }
        userRepository.delete(user);
    }

     /**
     * Deletes a User entity by ID using the UserRepository.
     *
     * @param userId The ObjectId of the user to delete.
     */
    public void deleteUserById(ObjectId userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null.");
        }
        userRepository.deleteById(userId);
    }

     /**
     * Checks if a user exists by username.
     * @param username The username to check.
     * @return true if a user with the username exists, false otherwise.
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByName(username);
    }

    /**
     * Checks if a user exists by email.
     * @param email The email to check.
     * @return true if a user with the email exists, false otherwise.
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
