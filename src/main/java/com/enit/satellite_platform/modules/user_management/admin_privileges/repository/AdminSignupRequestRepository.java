package com.enit.satellite_platform.modules.user_management.admin_privileges.repository;

import com.enit.satellite_platform.modules.user_management.models.AdminSignupRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminSignupRequestRepository extends MongoRepository<AdminSignupRequest, String> {

    List<AdminSignupRequest> findByStatus(AdminSignupRequest.ApprovalStatus status);

    Optional<AdminSignupRequest> findByEmailAndStatus(String email, AdminSignupRequest.ApprovalStatus status);

    boolean existsByEmailAndStatus(String email, AdminSignupRequest.ApprovalStatus status);
}
