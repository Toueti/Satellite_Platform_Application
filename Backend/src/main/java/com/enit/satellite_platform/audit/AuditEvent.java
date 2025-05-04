package com.enit.satellite_platform.audit;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "audit_events")
@Data
public class AuditEvent {
    @Id
    private String id;

    @Field("userId")
    private String userId;

    @Field("username")
    private String username;

    @Field("actionType")
    private String actionType;

    @Field("targetId")
    private String targetId;

    @Field("timestamp")
    private LocalDateTime timestamp;

    public AuditEvent() {
        this.timestamp = LocalDateTime.now();
    }
}
