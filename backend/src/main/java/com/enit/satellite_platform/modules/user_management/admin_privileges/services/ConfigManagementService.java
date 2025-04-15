package com.enit.satellite_platform.modules.user_management.admin_privileges.services;

import com.enit.satellite_platform.config.dto.ManageablePropertyDto;
import com.enit.satellite_platform.config.dto.UpdatePropertyRequestDto;
import com.enit.satellite_platform.config.environment.DatabasePropertySource;
import com.enit.satellite_platform.config.model.ConfigProperty;
import com.enit.satellite_platform.modules.user_management.admin_privileges.repository.ConfigPropertyRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.support.CronExpression;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ConfigManagementService {

    private static final Logger log = LoggerFactory.getLogger(ConfigManagementService.class);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final String RUNTIME_OVERRIDES_PROPERTY_SOURCE = "runtimeOverrides";

    private static final Set<String> VALID_LOG_LEVELS = Set.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL",
            "OFF"); // Added for log level validation

    @Autowired
    private ConfigPropertyRepository configPropertyRepository;

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired(required = false)
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private AuditLogService auditLogService;

    /**
     * A map of manageable properties and their descriptions.
     * This map is unmodifiable to prevent accidental changes at runtime.
     */
    // Note: The descriptions are for documentation purposes and can be used in the
    // UI or API responses.
    private final Map<String, String> MANAGEABLE_PROPERTIES;

    public ConfigManagementService() {
        Map<String, String> properties = new HashMap<>();

        // MongoDB Configuration
        properties.put("spring.data.mongodb.uri", "MongoDB connection URI");
        properties.put("logging.level.org.springframework.data.mongodb", "MongoDB logging level");
        properties.put("spring.data.mongodb.auto-index-creation", "Enable/disable automatic index creation");

        // JWT Configuration
        properties.put("jwt.secret", "Secret key for JWT token generation");
        properties.put("jwt.expiration", "JWT token expiration time in milliseconds");

        // Image Configuration
        properties.put("python.backend.url", "URL for the Python backend service");
        properties.put("storage.filesystem.directory", "Path for storing images");
        properties.put("thread.pool.size", "Size of the thread pool for processing tasks");

        // Spring Logging
        properties.put("logging.level.org.springframework", "Spring framework logging level");

        // Redis Configuration
        properties.put("spring.data.redis.url", "Redis connection URL");

        // Project Configuration
        properties.put("project.base.path", "Base path for the project");
        properties.put("springdoc.swagger-ui.path", "Path for Swagger UI");

        // Rate Limiting
        properties.put("rate_limit.max_requests", "Maximum requests allowed within the time window");
        properties.put("rate_limit.time_window_millis", "Time window for rate limiting in milliseconds");
        properties.put("rate-limit.cleanup-interval", "Interval for rate limit cleanup");

        // Analysis Configuration
        properties.put("analysis.valid_types", "List of valid analysis types (comma-separated)");

        // Cache Configuration
        properties.put("cache.redis.ttl_seconds", "Default Time-To-Live for Redis cache entries in seconds");
        properties.put("cache.redis.prefix", "Prefix for Redis cache keys");
        properties.put("cache.cleanup.max_infrequent_access_count",
                "Maximum number of times a cache entry can be infrequently accessed before being removed");
        properties.put("cache.cleanup.inactivity_threshold_days",
                "Number of days of inactivity before a cache entry is considered for removal");
        properties.put("cache.cleanup.cron", "Cron expression for scheduling cache cleanup");

        // Messaging Configuration - Attachments
        properties.put("messaging.attachments.max.size", "Maximum file size (in MB) for message attachments");
        properties.put("messaging.attachments.allowed.types",
                "Allowed attachment file formats (comma-separated list of extensions or MIME types)");
        // properties.put("messaging.attachments.storage.type", "Where to store
        // attachments (local or cloud) - Future use"); // Example for future
        // properties.put("messaging.attachments.storage.path", "Base file storage
        // directory (if using local storage) - Future use, currently uses
        // file.upload-dir"); // Example for future

        // Messaging Configuration - Retention & Cleanup
        properties.put("messaging.retention.days",
                "How long messages are stored before deletion (0 to disable retention-based cleanup)");
        properties.put("messaging.cleanup.enabled", "Enable/disable automatic message cleanup job");
        properties.put("messaging.max.messages.per.conversation",
                "Maximum messages per conversation before oldest are deleted (0 to disable count-based cleanup)");

        // Messaging Configuration - User Controls
        properties.put("messaging.user-to-user.enabled", "Enable/disable direct user-to-user messaging");
        properties.put("messaging.user-to-admin.enabled", "Enable/disable user-to-admin support chat");
        properties.put("messaging.user-to-bot.enabled", "Enable/disable chatbot interactions");

        // Messaging Configuration - RabbitMQ & Queue Settings
        properties.put("messaging.rabbitmq.enabled", "Globally enable/disable the messaging module");
        properties.put("messaging.rabbitmq.routing.user.direct",
                "Routing key for direct user messages (if using static key)"); // Example if not using userId
        properties.put("messaging.rabbitmq.routing.admin.topic",
                "Base routing key for admin topic messages (e.g., 'admin.message')");
        properties.put("messaging.rabbitmq.routing.bot.topic",
                "Base routing key for bot topic messages (e.g., 'bot.message')");
        properties.put("messaging.queue.dlx.enabled", "Enable Dead Letter Queue (DLQ) for failed messages");
        properties.put("messaging.queue.prefetch.count", "Number of messages a consumer processes at once");

        // File Upload Configuration
        // Maximum file size for uploads (e.g., images, documents)
        properties.put("spring.servlet.multipart.max-file-size", "Maximum file size for uploads (in MB)");
        properties.put("spring.servlet.multipart.max-request-size", "Maximum request size for uploads (in MB)");

        // TODO: Add other messaging properties here later

        MANAGEABLE_PROPERTIES = Collections.synchronizedMap(properties);
    }

    /**
     * Adds a new manageable property with its description
     * 
     * @param key         Property key
     * @param description Property description
     * @return true if added successfully, false if already exists
     *         Note: This method is not thread-safe and should be used with caution
     *         in a multi-threaded environment.
     *         It is recommended to initialize the manageable properties in the
     *         constructor or a static block.
     */
    public boolean addManageableProperty(String key, String description) {
        if (MANAGEABLE_PROPERTIES.containsKey(key)) {
            return false;
        }
        MANAGEABLE_PROPERTIES.put(key, description);
        return true;
    }

    /**
     * Retrieves all manageable configuration properties with their current and
     * default values.
     *
     * @return List of {@link ManageablePropertyDto} ManageablePropertyDto objects
     *         representing the list of manageable properties.
     */
    public List<ManageablePropertyDto> getManageableProperties() {
        List<ConfigProperty> overrides = configPropertyRepository.findAllById(MANAGEABLE_PROPERTIES.keySet());
        Map<String, ConfigProperty> overrideMap = overrides.stream()
                .collect(Collectors.toMap(ConfigProperty::getId, prop -> prop));

        return MANAGEABLE_PROPERTIES.entrySet().stream()
                .map(entry -> {
                    String key = entry.getKey();
                    // Always get description from the definitive MANAGEABLE_PROPERTIES map
                    String description = MANAGEABLE_PROPERTIES.getOrDefault(key, "No description available.");
                    ConfigProperty override = overrideMap.get(key);

                    // Get the value currently active in the environment (might include overrides
                    // already)
                    String environmentValue = environment.getProperty(key, "");

                    // Determine the current value to display
                    String currentValue;
                    if (override != null && override.getValue() != null) {
                        // If there's an active override in the DB (value is not null), that's the
                        // current value
                        currentValue = override.getValue();
                    } else {
                        // Otherwise, the value from the environment (which might be the original
                        // default or another override) is the current one
                        currentValue = environmentValue;
                    }

                    // Determine the default value to display
                    String defaultValue;
                    if (override != null && override.getDefaultValue() != null) {
                        // Use the default value stored in the ConfigProperty entity if available and
                        // not null
                        defaultValue = override.getDefaultValue();
                    } else {
                        // Fallback: If not stored in ConfigProperty or is null, use the current
                        // environment value
                        // This might not be the *original* default if other overrides exist.
                        defaultValue = environmentValue;
                        if (override != null) { // Log only if an override exists but lacks a default value
                            log.trace(
                                    "Default value for key '{}' not found in ConfigProperty override; using environment value '{}'.",
                                    key, environmentValue);
                        }
                    }

                    String lastUpdated = (override != null && override.getLastUpdated() != null)
                            ? override.getLastUpdated().format(ISO_FORMATTER)
                            : null;

                    return new ManageablePropertyDto(key, currentValue, defaultValue, description, lastUpdated);
                })
                .sorted(Comparator.comparing(ManageablePropertyDto::getKey))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all configuration properties that start with the specified prefix.
     * This includes their current and default values, along with metadata such as
     * description and last updated time.
     *
     * @param prefix The prefix to filter configuration properties by.
     * @return A List of {@link ManageablePropertyDto} ManageablePropertyDto objects
     *         representing the list of manageable properties matching the prefix.
     */
    public List<ManageablePropertyDto> getAllConfigs(String prefix) {
        List<ConfigProperty> overrides = configPropertyRepository.findAllById(MANAGEABLE_PROPERTIES.keySet());
        Map<String, ConfigProperty> overrideMap = overrides.stream()
                .collect(Collectors.toMap(ConfigProperty::getId, prop -> prop));

        return MANAGEABLE_PROPERTIES.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(prefix))
                .map(entry -> {
                    String key = entry.getKey();
                    // Always get description from the definitive MANAGEABLE_PROPERTIES map
                    String description = MANAGEABLE_PROPERTIES.getOrDefault(key, "No description available.");
                    ConfigProperty override = overrideMap.get(key);

                    // Get the value currently active in the environment (might include overrides
                    // already)
                    String environmentValue = environment.getProperty(key, "");

                    // Determine the current value to display
                    String currentValue;
                    if (override != null && override.getValue() != null) {
                        // If there's an active override in the DB (value is not null), that's the
                        // current value
                        currentValue = override.getValue();
                    } else {
                        // Otherwise, the value from the environment (which might be the original
                        // default or another override) is the current one
                        currentValue = environmentValue;
                    }

                    // Determine the default value to display
                    String defaultValue;
                    if (override != null && override.getDefaultValue() != null) {
                        // Use the default value stored in the ConfigProperty entity if available and
                        // not null
                        defaultValue = override.getDefaultValue();
                    } else {
                        // Fallback: If not stored in ConfigProperty or is null, use the current
                        // environment value
                        // This might not be the *original* default if other overrides exist.
                        defaultValue = environmentValue;
                        if (override != null) { // Log only if an override exists but lacks a default value
                            log.trace(
                                    "Default value for key '{}' not found in ConfigProperty override; using environment value '{}'.",
                                    key, environmentValue);
                        }
                    }

                    String lastUpdated = (override != null && override.getLastUpdated() != null)
                            ? override.getLastUpdated().format(ISO_FORMATTER)
                            : null;

                    return new ManageablePropertyDto(key, currentValue, defaultValue, description, lastUpdated);
                })
                .sorted(Comparator.comparing(ManageablePropertyDto::getKey))
                .collect(Collectors.toList());
    }

    /**
     * Updates a specific configuration property at runtime using the provided
     * value.
     * If the new value is null, the property will be reset to its original default
     * value.
     * If the property is already set to its default value, this method does
     * nothing.
     * If the property is not designated as manageable, an IllegalArgumentException
     * is thrown.
     * If the new value is invalid according to the property's validators, an
     * IllegalArgumentException is thrown.
     * If the property is successfully updated, the ConfigProperty entity is
     * returned, reflecting the new value.
     * The returned ConfigProperty instance always has the defaultValue field
     * populated.
     * If the property is reset to default, the returned ConfigProperty instance has
     * the value field set to the original default value.
     * If the property is already set to its default value, the returned
     * ConfigProperty instance is the existing entity.
     * If the property is not found in the DB, a new ConfigProperty entity is
     * created with the default value set.
     * The environment is always updated with the new value or the original default
     * value.
     * The DatabasePropertySource cache is updated with the new value or the
     * original default value if present.
     * If the event publisher is set, a refresh event is published with the updated
     * property key.
     * 
     * @param request the request containing the key and new value to update.
     * @return the updated {@link ConfigProperty} entity or representation of the
     *         reset state.
     */
    @Transactional
    public ConfigProperty updateProperty(UpdatePropertyRequestDto request) {
        String key = request.getKey();
        String newValue = request.getValue();

        if (!MANAGEABLE_PROPERTIES.containsKey(key)) {
            throw new IllegalArgumentException("Configuration property '" + key + "' is not designated as manageable.");
        }

        if (newValue != null) {
            validatePropertyValue(key, newValue);
        }

        // Fetch the original default value from the environment *before* applying the
        // potential new override.
        // This is the best guess for the original default at this point.
        String originalDefaultValue = environment.getProperty(key, "");

        ConfigProperty property = configPropertyRepository.findById(key)
                .orElse(new ConfigProperty(key, null, MANAGEABLE_PROPERTIES.get(key), originalDefaultValue)); // Pass
                                                                                                              // default
                                                                                                              // value

        // Ensure the defaultValue field is populated if the entity already exists but
        // lacks it
        if (property.getDefaultValue() == null) {
            property.setDefaultValue(originalDefaultValue);
        }

        if (newValue == null) { // Resetting to default
            if (configPropertyRepository.existsById(key)) {
                configPropertyRepository.deleteById(key);
                log.info("Reset configuration property '{}' to default by removing override.", key);
                // Return a representation of the reset state, using the fetched original
                // default
                return new ConfigProperty(key, originalDefaultValue, property.getDescription(), originalDefaultValue);
            } else {
                log.info("Configuration property '{}' was already using default value.", key);
                // Ensure the existing property representation has the default value set
                // correctly
                property.setValue(originalDefaultValue); // Reflect the current state is the default
                return property; // Return the existing property object which should now have the default value
            }
        } else {
            property.setValue(newValue);
            property.setLastUpdated(LocalDateTime.now());
            property = configPropertyRepository.save(property);
            log.info("Updated configuration property '{}' to value: '{}'", key, newValue);
        }

        DatabasePropertySource dbPropertySource = (DatabasePropertySource) environment.getPropertySources()
                .get(DatabasePropertySource.NAME);
        if (dbPropertySource != null) {
            dbPropertySource.refreshProperty(key, newValue);
        } else {
            log.warn("DatabasePropertySource not found; cache not updated.");
        }

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new EnvironmentChangeEvent(Collections.singleton(key)));
            log.debug("Refresh event not published; Spring Cloud Context not enabled.");
        }

        return property;
    }

    /**
     * Updates a specific configuration property at runtime.
     * 
     * @param updateRequest The request containing the key and value to update.
     */
    public void updateConfigurationProperty(UpdatePropertyRequestDto updateRequest) {
        if (updateRequest == null || updateRequest.getKey() == null || updateRequest.getKey().isBlank()) {
            throw new IllegalArgumentException("Configuration key cannot be null or blank.");
        }

        MutablePropertySources propertySources = environment.getPropertySources();
        Map<String, Object> map;

        if (propertySources.contains(RUNTIME_OVERRIDES_PROPERTY_SOURCE)) {
            // * Get existing map if property source exists
            MapPropertySource propertySource = (MapPropertySource) propertySources
                    .get(RUNTIME_OVERRIDES_PROPERTY_SOURCE);
            // * Need to create a new map as the underlying source map might be unmodifiable
            map = new HashMap<>(propertySource.getSource());
        } else {
            // *Create new map if property source doesn't exist
            map = new HashMap<>();
        }

        // Add or update the property
        map.put(updateRequest.getKey(), updateRequest.getValue());

        // Replace or add the property source with high precedence
        if (propertySources.contains(RUNTIME_OVERRIDES_PROPERTY_SOURCE)) {
            propertySources.replace(RUNTIME_OVERRIDES_PROPERTY_SOURCE,
                    new MapPropertySource(RUNTIME_OVERRIDES_PROPERTY_SOURCE, map));
        } else {
            // Add it just after systemProperties to ensure it overrides most other sources
            propertySources.addFirst(new MapPropertySource(RUNTIME_OVERRIDES_PROPERTY_SOURCE, map));
        }

        // Optionally: Log the change or trigger a refresh event if using Spring Cloud
        // Config
        log.info("Updated configuration property: {} = {}", updateRequest.getKey(), updateRequest.getValue()); // Use
                                                                                                               // logger
        // Audit Log
        auditLogService.logAuditEvent(AdminServices.getCurrentUsername(), "CONFIG_UPDATED",
                "Property updated: " + updateRequest.getKey() + "=" + updateRequest.getValue());
    }

    /**
     * Gets the effective value of a property, checking overrides first, then the
     * environment.
     * Primarily for internal use or potentially exposing read-only values.
     *
     * @param key The property key.
     * @return The effective value or null if not found.
     */
    public String getEffectivePropertyValue(String key) {
        return configPropertyRepository.findById(key)
                .map(ConfigProperty::getValue)
                .orElse(environment.getProperty(key));
    }

    /**
     * Validates the value for a given manageable property key.
     * Throws IllegalArgumentException if the value is invalid.
     *
     * @param key   The property key.
     * @param value The property value to validate.
     */
    private void validatePropertyValue(String key, String value) {
        if (value.trim().isEmpty()) {
            if ("jwt.secret".equals(key)) {
                throw new IllegalArgumentException("Property '" + key + "' cannot be empty.");
            }
            return; // Allow empty for others unless specified
        }

        try {
            switch (key) {
                case "spring.data.mongodb.uri":
                case "python.backend.url":
                case "spring.data.redis.url":
                    new URI(value);
                    break;
                case "logging.level.org.springframework.data.mongodb":
                case "logging.level.org.springframework":
                    if (!VALID_LOG_LEVELS.contains(value.toUpperCase())) {
                        throw new IllegalArgumentException(
                                "Invalid log level '" + value + "' for '" + key + "'. Must be: " + VALID_LOG_LEVELS);
                    }
                    break;
                case "spring.data.mongodb.auto-index-creation":
                    if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                        throw new IllegalArgumentException(
                                "Invalid boolean value '" + value + "' for '" + key + "'. Must be 'true' or 'false'.");
                    }
                    break;
                case "jwt.expiration":
                case "thread.pool.size":
                case "rate_limit.max_requests":
                case "rate_limit.time_window_millis":
                case "rate-limit.cleanup-interval":
                case "cache.redis.ttl_seconds":
                case "cache.cleanup.max_infrequent_access_count":
                case "cache.cleanup.inactivity_threshold_days":
                    long numValue = Long.parseLong(value);
                    if (numValue <= 0 || ("thread.pool.size".equals(key) && numValue == 0)) {
                        throw new IllegalArgumentException("Numeric value for '" + key + "' must be positive" +
                                ("thread.pool.size".equals(key) ? " and greater than 0." : "."));
                    }
                    break;
                case "storage.filesystem.directory":
                case "project.base.path":
                    Paths.get(value);
                    break;
                case "springdoc.swagger-ui.path":
                    if (!value.matches("/[a-zA-Z0-9/-]*")) {
                        throw new IllegalArgumentException("Swagger UI path '" + value
                                + "' must start with '/' and contain only letters, numbers, slashes, or hyphens.");
                    }
                    break;
                case "analysis.valid_types":
                    if (Arrays.stream(value.split(",")).allMatch(String::isBlank)) {
                        throw new IllegalArgumentException("Property '" + key + "' must contain non-empty values.");
                    }
                    break;
                case "cache.cleanup.cron":
                    if (!CronExpression.isValidExpression(value)) {
                        throw new IllegalArgumentException(
                                "Invalid cron expression '" + value + "' for '" + key + "'.");
                    }
                    break;
                case "jwt.secret":
                    if (value.length() < 16 || value.matches("[a-zA-Z0-9]+")) {
                        throw new IllegalArgumentException(
                                "JWT secret must be at least 16 characters and include special characters.");
                    }
                    break;
                case "cache.redis.prefix":
                    if (!value.matches("[a-zA-Z0-9:_-]+")) {
                        throw new IllegalArgumentException(
                                "Redis prefix must contain only letters, numbers, colons, underscores, or hyphens.");
                    }
                    break;
                // Messaging Attachment Validation
                case "messaging.attachments.max.size":
                    long maxSize = Long.parseLong(value);
                    if (maxSize <= 0) {
                        throw new IllegalArgumentException("Property '" + key + "' must be a positive number (MB).");
                    }
                    break;
                case "messaging.attachments.allowed.types":
                    if (value.isBlank() || Arrays.stream(value.split(",")).allMatch(String::isBlank)) {
                        throw new IllegalArgumentException("Property '" + key
                                + "' must contain a non-empty comma-separated list of types/extensions.");
                    }
                    // Basic check, more specific validation (e.g., valid MIME types) could be added
                    break;
                // Messaging Retention Validation
                case "messaging.retention.days":
                case "messaging.max.messages.per.conversation":
                    long retentionValue = Long.parseLong(value);
                    if (retentionValue < 0) {
                        throw new IllegalArgumentException("Property '" + key + "' must be a non-negative number.");
                    }
                    break;
                case "messaging.cleanup.enabled":
                case "messaging.user-to-user.enabled":
                case "messaging.user-to-admin.enabled":
                case "messaging.user-to-bot.enabled":
                    if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                        throw new IllegalArgumentException(
                                "Invalid boolean value '" + value + "' for '" + key + "'. Must be 'true' or 'false'.");
                    }
                    break;
                // case "messaging.max.messages.per.user.daily": // Add later if implementing
                // rate limiting
                // long dailyLimit = Long.parseLong(value);
                // if (dailyLimit < 0) {
                // throw new IllegalArgumentException("Property '" + key + "' must be a
                // non-negative number.");
                // }
                // break;
                // Messaging RabbitMQ/Queue Validation
                case "messaging.rabbitmq.enabled":
                case "messaging.queue.dlx.enabled":
                    if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                        throw new IllegalArgumentException(
                                "Invalid boolean value '" + value + "' for '" + key + "'. Must be 'true' or 'false'.");
                    }
                    break;
                case "messaging.queue.prefetch.count":
                    long prefetch = Long.parseLong(value);
                    if (prefetch <= 0) {
                        throw new IllegalArgumentException("Property '" + key + "' must be a positive number.");
                    }
                    break;
                case "messaging.rabbitmq.routing.user.direct":
                case "messaging.rabbitmq.routing.admin.topic":
                case "messaging.rabbitmq.routing.bot.topic":
                    // Basic validation: not blank. More specific routing key validation could be
                    // added.
                    if (value.isBlank()) {
                        throw new IllegalArgumentException("Routing key property '" + key + "' cannot be blank.");
                    }
                    // Example: check for invalid characters if needed
                    // if (!value.matches("^[a-zA-Z0-9._-]+$")) { ... }
                    break;
                // TODO: Add validation for other messaging properties later
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric format '" + value + "' for '" + key + "'.");
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(
                    "Invalid URI format '" + value + "' for '" + key + "': " + e.getMessage());
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException(
                    "Invalid path format '" + value + "' for '" + key + "': " + e.getMessage());
        }

        auditLogService.logAuditEvent(AdminServices.getCurrentUsername(), "CONFIG_VALIDATION",
                "Property validated: " + key + "=" + value);
        log.info("Validated configuration property: {} = {}", key, value);
    }

}
