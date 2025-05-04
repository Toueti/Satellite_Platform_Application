package com.enit.satellite_platform.modules.user_management.admin_privileges.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.AdminSignupRequest;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminSignupRequestRepository extends MongoRepository<AdminSignupRequest, String> {
    /**
     * Finds all AdminSignupRequests with the specified status.
     *
     * @param status the status of the AdminSignupRequest
     * @return a list of AdminSignupRequests with the specified status
     */
    List<AdminSignupRequest> findByStatus(AdminSignupRequest.ApprovalStatus status);

    /**
     * Finds an AdminSignupRequest by email and status.
     *
     * @param email    the email of the AdminSignupRequest
     * @param status   the status of the AdminSignupRequest
     * @return an Optional containing the AdminSignupRequest if found, or empty if not found
     */
    Optional<AdminSignupRequest> findByEmailAndStatus(String email, AdminSignupRequest.ApprovalStatus status);

    /**
     * Checks if an AdminSignupRequest with the specified email and status exists.
     *
     * @param email    the email of the AdminSignupRequest
     * @param status   the status of the AdminSignupRequest
     * @return true if an AdminSignupRequest with the specified email and status exists, false otherwise
     */
    boolean existsByEmailAndStatus(String email, AdminSignupRequest.ApprovalStatus status);
}
