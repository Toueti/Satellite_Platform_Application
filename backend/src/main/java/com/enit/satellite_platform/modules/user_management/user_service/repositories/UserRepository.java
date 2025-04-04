package com.enit.satellite_platform.modules.user_management.user_service.repositories;

import org.bson.types.ObjectId;
import com.enit.satellite_platform.modules.user_management.models.Authority; // Added import
import org.springframework.data.mongodb.repository.MongoRepository;

import com.enit.satellite_platform.modules.user_management.models.User;
import org.springframework.stereotype.Repository; // Added import

import java.util.List; // Added import
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, ObjectId> {

    Optional<User> findByEmail(String email);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    void deleteByEmail(String email);

    Optional<User> findByResetToken(String resetToken);

    // Check if any user has the specified authority in their authorities list
    boolean existsByAuthoritiesContains(Authority authority);

    // Find all users that have the specified authority in their authorities list
    List<User> findByAuthoritiesContains(Authority authority); // Added method
}
