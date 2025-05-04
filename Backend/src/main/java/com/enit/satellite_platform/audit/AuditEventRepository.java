package com.enit.satellite_platform.audit;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import lombok.Value; // Import Lombok annotation for DTO

import java.util.List;
// import java.util.Map; // No longer needed for this method
import java.time.LocalDateTime;

@Repository
public interface AuditEventRepository extends MongoRepository<AuditEvent, String> {

    @Value
    class ActionFrequencyDto {
        String id;
        long count;
    }

    // Find events by user ID, ordered by timestamp descending
    List<AuditEvent> findByUserIdOrderByTimestampDesc(String userId);

    // Find events by user ID and action type, ordered by timestamp descending
    List<AuditEvent> findByUserIdAndActionTypeOrderByTimestampDesc(String userId, String actionType);

    // Find events within a specific time range for a user
    List<AuditEvent> findByUserIdAndTimestampBetweenOrderByTimestampDesc(String userId, LocalDateTime start, LocalDateTime end);

    // Count events by user ID and action type
    long countByUserIdAndActionType(String userId, String actionType);

    // Find the most recent event for a user and action type
    AuditEvent findTopByUserIdAndActionTypeOrderByTimestampDesc(String userId, String actionType);

    // Find the top N events for a user, ordered by timestamp descending
    List<AuditEvent> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable); // Renamed for clarity and consistency

    // Count events for a user within a specific time range
    long countByUserIdAndTimestampBetween(String userId, LocalDateTime start, LocalDateTime end);

    // Aggregate to find the most frequent action type for a user
    @Aggregation(pipeline = {
            "{ $match: { userId: ?0 } }", // Filter by userId
            "{ $group: { _id: '$actionType', count: { $sum: 1 } } }", // Group by actionType and count occurrences
            "{ $sort: { count: -1 } }", // Sort by count descending
            "{ $limit: 1 }" // Take the top one
    })
    ActionFrequencyDto findMostFrequentActionTypeByUserId(String userId); // Changed return type to the specific DTO
}
