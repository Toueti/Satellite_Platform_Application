package com.enit.satellite_platform.modules.activity.repository;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.enit.satellite_platform.modules.activity.entities.ActivityLog;

import java.time.LocalDateTime;

@Repository
public interface ActivityLogRepository extends MongoRepository<ActivityLog, ObjectId> {

    // Find logs for a specific user, ordered by timestamp descending
    Page<ActivityLog> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);

    // Find logs for a specific username (email), ordered by timestamp descending
    Page<ActivityLog> findByUsernameOrderByTimestampDesc(String username, Pageable pageable);

    // Find logs by action type, ordered by timestamp descending
    Page<ActivityLog> findByActionOrderByTimestampDesc(String action, Pageable pageable);

    // Find logs within a time range, ordered by timestamp descending
    Page<ActivityLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end, Pageable pageable);

    // Find logs for a user within a time range
    Page<ActivityLog> findByUserIdAndTimestampBetweenOrderByTimestampDesc(String userId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    // Find logs for a user with a specific action
    Page<ActivityLog> findByUserIdAndActionOrderByTimestampDesc(String userId, String action, Pageable pageable);
}
