package com.enit.satellite_platform.modules.activity.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "activity_logs")
@Data
@NoArgsConstructor
public class ActivityLog {

    @Id
    private ObjectId id;

    @Indexed
    @Field("user_id")
    private String userId;

    @Indexed
    @Field("username")
    private String username;

    @Indexed
    @Field("action")
    private String action;

    @Field("details")
    private String details;

    @CreatedDate
    @Field("timestamp")
    private LocalDateTime timestamp;

    public ActivityLog(String userId, String username, String action, String details) {
        this.userId = userId;
        this.username = username;
        this.action = action;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }
}
