package com.enit.satellite_platform.modules.user_management.repositories;

import com.enit.satellite_platform.modules.user_management.models.Authority;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthorityRepository extends MongoRepository<Authority, ObjectId> {
    Optional<Authority> findByAuthority(String authority);
    boolean existsByAuthority(String authority);
}
