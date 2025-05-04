package com.enit.satellite_platform.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditServiceImpl implements AuditService {

    private static final Logger logger = LoggerFactory.getLogger("com.enit.satellite_platform.audit");

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Override
    @Transactional // Make the operation transactional
    public void recordEvent(AuditEvent event) {
        if (event == null) {
            logger.warn("Attempted to record a null audit event.");
            return;
        }
        try {
            auditEventRepository.save(event);
            logger.info("Audit event recorded: User={}, Action={}, Target={}",
                         event.getUsername(), event.getActionType(), event.getTargetId());
        } catch (Exception e) {
            // Log error but don't let audit failure break the main operation
            logger.error("Failed to record audit event for user {}: {}", event.getUsername(), e.getMessage(), e);
        }
    }

    @Override
    public void recordEvent(String userId, String username, String actionType, String targetId) {
        if (username == null || actionType == null) {
             logger.warn("Attempted to record an audit event with null username or actionType. UserID: {}", userId);
             return;
        }
        AuditEvent event = new AuditEvent();
        event.setUserId(userId);
        event.setUsername(username);
        event.setActionType(actionType);
        event.setTargetId(targetId);

        recordEvent(event);
    }
}
