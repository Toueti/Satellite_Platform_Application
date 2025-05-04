package com.enit.satellite_platform.modules.user_management.normal_user_service.repositories;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository; // Added import

import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.Authority;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.User;

import java.util.List; // Added import
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, ObjectId> {

    Optional<User> findByEmail(String email);

    // Renamed from existsByUsername to match the 'name' property in User entity
    Boolean existsByName(String name);

    Boolean existsByEmail(String email);

    void deleteByEmail(String email);

    Optional<User> findByResetToken(String resetToken);

    // Check if any user has the specified authority in their authorities list
    boolean existsByAuthoritiesContains(Authority authority);

    // Find all users that have the specified authority in their authorities list
    List<User> findByAuthoritiesContains(Authority authority); // Added method

    default boolean isAdmin(ObjectId userObjectId) {
        Optional<User> userOpt = findById(userObjectId);
        return userOpt.map(user -> user.getAuthorities().stream()
                .anyMatch(auth -> "ADMIN".equals(auth.getAuthority())))
                .orElse(false);
    }
}
