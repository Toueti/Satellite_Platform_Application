package com.enit.satellite_platform.modules.user_management.management_cvore_service.security.device;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing DeviceInfo entities in MongoDB.
 */
@Repository
public interface DeviceInfoRepository extends MongoRepository<DeviceInfo, String> {
    
    /**
     * Find a device by its unique identifier and user ID.
     */
    Optional<DeviceInfo> findByDeviceIdentifierAndUserId(String deviceIdentifier, String userId);
    
    /**
     * Find all devices belonging to a specific user.
     */
    List<DeviceInfo> findByUserId(String userId);
    
    /**
     * Find all active devices for a user (devices used within the last 30 days).
     */
    @Query("{'userId': ?0, 'lastUsedAt': {$gte: ?1}}")
    List<DeviceInfo> findActiveDevicesByUserId(String userId, LocalDateTime thirtyDaysAgo);
    
    /**
     * Find all approved devices for a user.
     */
    List<DeviceInfo> findByUserIdAndIsApprovedTrue(String userId);
    
    /**
     * Count active devices for a user.
     */
    @Query(value = "{'userId': ?0, 'lastUsedAt': {$gte: ?1}}", count = true)
    long countActiveDevicesByUserId(String userId, LocalDateTime thirtyDaysAgo);
    
    /**
     * Find devices by IP address within a specific timeframe.
     * Useful for detecting suspicious activity from the same IP.
     */
    @Query("{'ipAddress': ?0, 'lastUsedAt': {$gte: ?1}}")
    List<DeviceInfo> findRecentDevicesByIpAddress(String ipAddress, LocalDateTime since);
    
    /**
     * Delete all devices for a specific user.
     * Used when a user account is deleted.
     */
    void deleteByUserId(String userId);
    
    /**
     * Find devices that haven't been used recently.
     * Useful for cleanup of inactive devices.
     */
    @Query("{'lastUsedAt': {$lt: ?0}}")
    List<DeviceInfo> findInactiveDevices(LocalDateTime cutoffDate);
}
