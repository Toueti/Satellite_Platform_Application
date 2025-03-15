package com.enit.satellite_platform.user_management.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.enit.satellite_platform.user_management.model.Authority;

import java.util.Optional;

public interface AuthorityRepository extends MongoRepository<Authority, ObjectId> {
    Optional<Authority> findByAuthority(String authority);
}
