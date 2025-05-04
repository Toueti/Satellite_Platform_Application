package com.enit.satellite_platform.modules.user_management.admin_privileges.repository;

import com.enit.satellite_platform.config.model.ConfigProperty;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfigPropertyRepository extends MongoRepository<ConfigProperty, String> {
    /**
     * Finds a ConfigProperty by its key.
     *
     * @param key the key of the ConfigProperty
     * @return an Optional containing the ConfigProperty if found, or empty if not found
     */
    Optional<ConfigProperty> findById(String key);
}
