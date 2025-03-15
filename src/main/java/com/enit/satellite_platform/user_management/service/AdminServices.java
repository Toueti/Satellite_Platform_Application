package com.enit.satellite_platform.user_management.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.enit.satellite_platform.user_management.exception.RoleNotFoundException;
import com.enit.satellite_platform.user_management.exception.UserAlreadyExistsException;
import com.enit.satellite_platform.user_management.model.Authority;
import com.enit.satellite_platform.user_management.model.User;
import com.enit.satellite_platform.user_management.repository.AuthorityRepository;
import com.enit.satellite_platform.user_management.repository.UserRepository;

@Service
public class AdminServices {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Creates a new user with the specified username, email, password, and roles.
     *
     * @param username The username for the new user.
     * @param email The email address for the new user.
     * @param password The password for the new user.
     * @param roleNames A set of role names to assign to the new user.
     * @return The created User object.
     * @throws UserAlreadyExistsException If the username or email is already in use.
     * @throws RoleNotFoundException If any of the specified roles are not found.
     */

    public User createUser(String username, String email, String password, Set<String> roleNames) {
        // Check if username or email is already taken
        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("Username is already taken!");
        }
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Email is already in use!");
        }

        // Create the user
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));

        // Assign roles
        Set<Authority> roles = new HashSet<>();
        for (String roleName : roleNames) {
            Authority role = authorityRepository.findByAuthority(roleName)
                    .orElseThrow(() -> new RoleNotFoundException("Role not found: " + roleName));
            roles.add(role);
        }
        user.setAuthorities(roles);

        return userRepository.save(user);
    }

/**
 * Updates an existing user's information, including username, email, and roles.
 *
 * @param userId The ID of the user to update.
 * @param username The new username for the user.
 * @param email The new email address for the user.
 * @param roleNames A set of role names to assign to the user.
 * @return The updated User object.
 * @throws UsernameNotFoundException If the user with the given ID is not found.
 * @throws UserAlreadyExistsException If the new username or email is already taken by another user.
 * @throws RoleNotFoundException If any of the specified roles are not found.
 */

    public User updateUser(ObjectId userId, String username, String email, Set<String> roleNames) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        // Check if new username or email is taken by another user
        if (!user.getUsername().equals(username) && userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("Username is already taken!");
        }
        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Email is already in use!");
        }

        // Update fields
        user.setUsername(username);
        user.setEmail(email);

        // Update roles
        Set<Authority> roles = new HashSet<>();
        for (String roleName : roleNames) {
            Authority role = authorityRepository.findByAuthority(roleName)
                    .orElseThrow(() -> new RoleNotFoundException("Role not found: " + roleName));
            roles.add(role);
        }
        user.setAuthorities(roles);

        return userRepository.save(user);
    }

    /**
     * Resets the password for the user with the given ID.
     *
     * @param userId The ID of the user to reset the password for.
     * @param newPassword The new password for the user.
     * @throws UsernameNotFoundException If the user with the given ID is not found.
     */
    public void resetUserPassword(ObjectId userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

/**
 * Deletes a user by their ID.
 *
 * @param userId The ID of the user to delete.
 * @throws UsernameNotFoundException If the user with the given ID is not found.
 */

    public void deleteUser(String userId) {
        ObjectId objectId = new ObjectId(userId);
        User user = userRepository.findById(objectId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
        userRepository.delete(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(String id) {
        ObjectId objectId = new ObjectId(id);
        return userRepository.findById(objectId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
    }

}
