package com.enit.satellite_platform.user_management.service;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.enit.satellite_platform.project_management.model.Project;
import com.enit.satellite_platform.user_management.dto.JwtResponse;
import com.enit.satellite_platform.user_management.dto.LoginRequest;
import com.enit.satellite_platform.user_management.dto.SignUpRequest;
import com.enit.satellite_platform.user_management.dto.UserUpdateRequest;
import com.enit.satellite_platform.user_management.exception.InvalidCredentialsException;
import com.enit.satellite_platform.user_management.exception.RoleNotFoundException;
import com.enit.satellite_platform.user_management.exception.UserAlreadyExistsException;
import com.enit.satellite_platform.user_management.model.Authority;
import com.enit.satellite_platform.user_management.model.User;
import com.enit.satellite_platform.user_management.repository.AuthorityRepository;
import com.enit.satellite_platform.user_management.repository.UserRepository;
import com.enit.satellite_platform.user_management.security.JwtUtil;
import com.enit.satellite_platform.project_management.repository.ProjectRepository;
import com.enit.satellite_platform.resources_management.repositories.ImageRepository;

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

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

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
     * @throws UserAlreadyExistsException If the email is already in use, indicating
     *                                    a duplicate registration attempt.
     */
    public void addUser(SignUpRequest signUpRequest) {
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));

        Set<Authority> roles = new HashSet<>();
        String roleInput = signUpRequest.getRole();
        if (roleInput != null && !roleInput.equalsIgnoreCase("THEMATICIAN")) {
            throw new IllegalArgumentException("Only THEMATICIAN role is allowed during signup");
        }

        Authority defaultRole = authorityRepository.findByAuthority(Authority.ROLE_THEMATICIAN)
                .orElseThrow(() -> new RoleNotFoundException("Default role not found!"));
        roles.add(defaultRole);
        user.setAuthorities(roles);

        try {
            userRepository.save(user);
            logger.info("User signed up successfully with email: {}", signUpRequest.getEmail());
        } catch (DataIntegrityViolationException e) {
            logger.warn("Duplicate email '{}' attempted during signup", signUpRequest.getEmail());
            throw new UserAlreadyExistsException("Email '" + signUpRequest.getEmail() + "' is already in use! Please use a different email Or login instead.");
        }
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
    public JwtResponse accessUserAcount(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(), // Using email as username
                            loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtUtil.generateToken(userDetails);

            logger.info("User signed in successfully with email: {}", userDetails.getUsername());
            return new JwtResponse(token);
        } catch (BadCredentialsException e) {
            logger.warn("Invalid login attempt for username: {}", loginRequest.getUsername());
            throw new InvalidCredentialsException("Invalid username or password");
        }
    }

    /**
     * Updates the password for a user with the given ID, provided the old password
     * matches.
     * This method first finds the user with the given ID and checks if the old
     * password matches.
     * If the old password is valid, the password is updated with the new password.
     * If the new password is in use by another user, a UserAlreadyExistsException
     * is thrown.
     *
     * @param id          The ID of the user to update.
     * @param oldPassword The old password to check against.
     * @param newPassword The new password to set.
     * @throws UsernameNotFoundException   If the user with the given ID is not
     *                                     found.
     * @throws InvalidCredentialsException If the old password provided is
     *                                     incorrect.
     * @throws UserAlreadyExistsException  If the email is already in use.
     */
    public void updatePassword(ObjectId id, String oldPassword, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid old password");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        try {
            userRepository.save(user);
            logger.info("Password updated for user with id: {}", id);
        } catch (DataIntegrityViolationException e) {
            logger.warn("Duplicate email '{}' attempted during password update", user.getEmail());
            throw new UserAlreadyExistsException("Email '" + user.getEmail() + "' is already in use!");
        }
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
     * @throws UserAlreadyExistsException  If the email is already in use by another
     *                                     user.
     * @throws SecurityException           If the update is attempted by a user
     *                                     other than the one being updated.
     */
    public JwtResponse updateUser(ObjectId userId, UserUpdateRequest updatedUser) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = auth.getName(); // Email from JWT token
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        if (!user.getEmail().equals(currentUserEmail)) {
            throw new SecurityException("You can only update your own account");
        }

        if (updatedUser.getOldPassword() != null) {
            if (!passwordEncoder.matches(updatedUser.getOldPassword(), user.getPassword())) {
                throw new InvalidCredentialsException("Invalid old password");
            }
        } else {
            throw new InvalidCredentialsException("Old password is required to update user details");
        }

        user.setUsername(updatedUser.getUsername());
        user.setEmail(updatedUser.getEmail());
        if (updatedUser.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }

        try {
            userRepository.save(user);
            logger.info("User updated: {}", userId);
        } catch (DataIntegrityViolationException e) {
            logger.warn("Duplicate email '{}' attempted during user update", updatedUser.getEmail());
            throw new UserAlreadyExistsException("Email '" + updatedUser.getEmail() + "' is already in use!");
        }

        String newToken = jwtUtil.generateToken(user);
        return new JwtResponse(newToken);
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("User not found with id: {}", userId);
                    return new UsernameNotFoundException("User not found with id: " + userId);
                });

        // Find and delete all projects owned by this user
        List<Project> ownedProjects = projectRepository.findByOwner(user);
        if (!ownedProjects.isEmpty()) {
            logger.info("Deleting {} projects owned by user with id: {}", ownedProjects.size(), userId);
            for (Project project : ownedProjects) {
                imageRepository.deleteAllByProject_ProjectID(project.getProjectID());
            }
            projectRepository.deleteAll(ownedProjects);
        } else {
            logger.info("No projects found for user with id: {}", userId);
        }

        // Delete the user
        userRepository.deleteById(userId);
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
        return userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
    }

    public void resetPassword(String email) {
        // TODO: Implement reset password logic (e.g., send email with reset link)
        logger.info("Password reset requested for email: {}", email);
    }
}
