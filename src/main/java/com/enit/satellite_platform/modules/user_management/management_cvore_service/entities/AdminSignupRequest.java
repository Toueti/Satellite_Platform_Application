package com.enit.satellite_platform.modules.user_management.management_cvore_service.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "admin_signup_requests")
@Data
@NoArgsConstructor
public class AdminSignupRequest {

    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    @Id
    private String id;

    private String username;
    private String email;
    private String encodedPassword; // Store the already encoded password
    private LocalDateTime requestedTimestamp;
    private ApprovalStatus status = ApprovalStatus.PENDING;

    private String approvedByUserId; // ID of the admin who approved/rejected
    private LocalDateTime decisionTimestamp; // Timestamp of approval/rejection

    public AdminSignupRequest(String username, String email, String encodedPassword) {
        this.username = username;
        this.email = email;
        this.encodedPassword = encodedPassword;
        this.requestedTimestamp = LocalDateTime.now();
        this.status = ApprovalStatus.PENDING;
    }
}
