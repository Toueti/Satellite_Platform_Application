package com.enit.satellite_platform.modules.user_management.management_cvore_service.repositories;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.Authority;

import java.util.Optional;

@Repository
public interface AuthorityRepository extends MongoRepository<Authority, ObjectId> {
    Optional<Authority> findByAuthority(String authority);
    boolean existsByAuthority(String authority);
}
