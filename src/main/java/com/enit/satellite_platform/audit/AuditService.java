package com.enit.satellite_platform.audit;

public interface AuditService {

    /**
     * Records an audit event.
     *
     * @param event The AuditEvent object to record.
     */
    void recordEvent(AuditEvent event);

    /**
     * Records an audit event with specific details.
     *
     * @param userId     The ID of the user performing the action.
     * @param username   The username of the user performing the action.
     * @param actionType The type of action performed (e.g., LOGIN_SUCCESS).
     * @param targetId   The ID of the entity being acted upon (optional).
     */
    void recordEvent(String userId, String username, String actionType, String targetId);
}
