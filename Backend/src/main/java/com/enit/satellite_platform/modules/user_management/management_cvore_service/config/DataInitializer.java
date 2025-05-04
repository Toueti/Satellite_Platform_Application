package com.enit.satellite_platform.modules.user_management.management_cvore_service.config;

import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.Authority;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.User;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.repositories.AuthorityRepository;
import com.enit.satellite_platform.modules.user_management.normal_user_service.repositories.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;


@Component
public class DataInitializer implements ApplicationRunner { // Changed interface

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    private static final String ADMIN_ROLE_NAME = "ROLE_ADMIN";

    @Autowired
    private AuthorityRepository authorityRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired // Restore Autowired annotation
    private PasswordEncoder passwordEncoder;
    @Autowired
    private Environment environment; // Inject Environment

    // Use Environment to read properties, allowing .env override
    // @Value annotations might resolve too early or not reflect .env depending on loading order.
    // Environment.getProperty provides a more reliable way to check at runtime.

    // Keep @Value for enabled flag as it's less likely to be in .env
    @Value("${app.initial.admin.enabled:false}")
    private boolean initialAdminEnabled;

    // Remove @Value for details, will fetch from Environment
    // @Value("${app.initial.admin.username:}")
    // private String initialAdminUsername;
    // @Value("${app.initial.admin.email:}")
    // private String initialAdminEmail;
    // @Value("${app.initial.admin.password:}")
    // private String initialAdminPassword;


    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception { // Changed signature
        // Ensure roles exist
        Authority adminRole = ensureRoleExists(ADMIN_ROLE_NAME);
        ensureRoleExists("ROLE_THEMATICIAN");

        // Create initial admin if enabled
        if (initialAdminEnabled) {
            // Check if any user already has the ADMIN role
            if (userRepository.existsByAuthoritiesContains(adminRole)) {
                logger.debug("Admin user(s) already exist. Skipping initial admin creation."); // Changed log level
                return; // Exit if admin already exists
            }

            // --- Get Initial Admin Details (Prioritize .env -> application.properties -> Command Line Override) ---

            // 1. Get values using Environment (respects .env > application.properties)
            String usernameFromEnv = environment.getProperty("app.initial.admin.username", "");
            String emailFromEnv = environment.getProperty("app.initial.admin.email", "");
            String passwordFromEnv = environment.getProperty("app.initial.admin.password", "");

            // 2. Check for command-line overrides
            String username = getCommandLineOption(args, "initialAdminUsername", usernameFromEnv);
            String email = getCommandLineOption(args, "initialAdminEmail", emailFromEnv);
            String password = getCommandLineOption(args, "initialAdminPassword", passwordFromEnv);


            // Log source of credentials (excluding password)
            logCredentialSource(args, "initialAdminUsername", username, usernameFromEnv);
            logCredentialSource(args, "initialAdminEmail", email, emailFromEnv);
            // Do not log password source or value

            if (username.isBlank() || email.isBlank() || password.isBlank()) {
                logger.warn("Initial admin creation is enabled but required details (username, email, password) were not provided via command line, .env, or application properties. Skipping.");
                return;
            }

            // Security Warning for command-line password
            if (args.containsOption("initialAdminPassword")) {
                logger.warn("SECURITY WARNING: Providing initial admin password via command line argument is insecure and should only be used in controlled environments.");
            }

            logger.info("No admin users found. Attempting to create initial admin user from provided details...");
            createInitialAdmin(adminRole, username, email, password);

        } else {
            logger.debug("Initial admin creation via configuration/command-line is disabled (app.initial.admin.enabled=false)."); // Changed log level
        }
    }

    private Authority ensureRoleExists(String roleName) {
        return authorityRepository.findByAuthority(roleName).orElseGet(() -> {
            logger.info("Creating role: {}", roleName);
            Authority newRole = new Authority();
            newRole.setAuthority(roleName);
            return authorityRepository.save(newRole);
        });
    }

    // Helper to get command-line option or fallback to Environment/Properties value
    private String getCommandLineOption(ApplicationArguments args, String optionName, String fallbackValue) {
        if (args.containsOption(optionName)) {
            java.util.List<String> values = args.getOptionValues(optionName);
            if (values != null && !values.isEmpty() && !values.get(0).isBlank()) {
                return values.get(0); // Return command-line value if present and not blank
            }
        }
        return fallbackValue; // Otherwise return the value from Environment/Properties
    }

    // Helper to log the source of the credential
    private void logCredentialSource(ApplicationArguments args, String optionName, String finalValue, String envValue) {
        String keyName = optionName.replace("initialAdmin", "").toLowerCase(); // e.g., "username"
        if (args.containsOption(optionName) && getCommandLineOption(args, optionName, "").equals(finalValue)) {
             logger.debug("Using command-line argument for initial admin {}.", keyName);
        } else if (!envValue.isBlank() && envValue.equals(finalValue)) {
             // Check if the value came from Environment (could be .env or application.properties)
             // We can't definitively distinguish .env from application.properties here without more complex checks,
             // but Environment.getProperty respects the standard precedence.
             logger.debug("Using environment/property source for initial admin {}.", keyName);
        } else if (finalValue.isBlank()) {
             logger.debug("Initial admin {} not provided.", keyName);
        } else {
             // This case might occur if the fallback was empty but somehow finalValue isn't,
             // or if command-line arg was blank. Log generically.
             logger.debug("Determined initial admin {} from available sources.", keyName);
        }
    }


    private void createInitialAdmin(Authority adminRole, String username, String email, String password) {
        // Double-check for existing email/username before creating
        if (userRepository.existsByEmail(email)) {
             logger.warn("Initial admin creation failed: Email '{}' already exists.", email);
             return;
        }
        // Corrected to use the actual Java property name 'name'
        if (userRepository.existsByName(username)) {
             logger.warn("Initial admin creation failed: Username '{}' already exists.", username);
             return;
        }

        User adminUser = new User();
        adminUser.setName(username);
        adminUser.setEmail(email);
        adminUser.setPassword(passwordEncoder.encode(password));
        adminUser.setAuthorities(Set.of(adminRole));
        adminUser.setEnabled(true); // Ensure the initial admin is enabled
        adminUser.setLocked(false); // Ensure the initial admin is not locked

        try {
            userRepository.save(adminUser);
            logger.info("Successfully created initial admin user with email: {}", email);
        } catch (Exception e) {
            logger.error("Failed to create initial admin user with email {}: {}", email, e.getMessage(), e);
        }
    }
}
