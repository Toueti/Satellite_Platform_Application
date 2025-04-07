package com.enit.satellite_platform.modules.user_management.admin_privileges.repository;

import com.enit.satellite_platform.config.model.ConfigProperty;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfigPropertyRepository extends MongoRepository<ConfigProperty, String> {

    // Find by key (which is the ID)
    Optional<ConfigProperty> findById(String key);

    // You can add more specific finders if needed later
}
