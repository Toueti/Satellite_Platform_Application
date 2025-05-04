package com.enit.satellite_platform.config.environment;

import com.enit.satellite_platform.config.model.ConfigProperty;
import com.enit.satellite_platform.modules.user_management.admin_privileges.repository.ConfigPropertyRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertySource;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DatabasePropertySource extends PropertySource<ConfigPropertyRepository> {

    private static final Logger log = LoggerFactory.getLogger(DatabasePropertySource.class);
    public static final String NAME = "databaseConfigProperties";

    private final Map<String, Object> propertiesCache = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;

    public DatabasePropertySource(ConfigPropertyRepository source) {
        super(NAME, source);
    }

    public void initialize() {
        if (initialized) {
            return;
        }
        synchronized (this) {
            if (initialized) {
                return;
            }
            try {
                log.info("Initializing DatabasePropertySource: Loading properties from database...");
                List<ConfigProperty> props = getSource().findAll();
                propertiesCache.putAll(props.stream()
                        .filter(p -> p.getValue() != null) // Retain your null filtering
                        .collect(Collectors.toMap(ConfigProperty::getId, ConfigProperty::getValue)));
                log.info("Loaded {} properties from database into DatabasePropertySource cache.", propertiesCache.size());
                initialized = true;
            } catch (Exception e) {
                log.error("Failed to initialize DatabasePropertySource from database. Retrying on next access.", e);
                // Don’t mark initialized on failure, allowing retries (my improvement)
            }
        }
    }

    @Override
    public Object getProperty(String name) {
        if (!initialized) {
            initialize();
        }
        return propertiesCache.get(name);
    }

    public void refreshProperty(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Property key cannot be null or blank");
        }
        if (value == null) {
            if (propertiesCache.remove(key) != null) {
                log.debug("Removed property '{}' from cache", key); // Your debug-level logging
            }
        } else {
            propertiesCache.put(key, value);
            log.debug("Updated property '{}' with value '{}' in cache", key, value); // Your debug-level logging
        }
    }
}