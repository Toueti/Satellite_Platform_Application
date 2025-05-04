package com.enit.satellite_platform.shared.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    /**
     * Sends a notification to a user via email.
     *
     * @param email   The recipient's email address
     * @param subject The subject of the notification
     * @param message The message content
     */
    public void sendNotification(String email, String subject, String message) {
        logger.info("Sending notification to {}: {} - {}", email, subject, message);
        // TODO: Implement actual email sending logic
        // This could be implemented using JavaMailSender or any other email service
    }

    /**
     * Sends an alert notification to a user.
     *
     * @param email   The recipient's email address
     * @param message The alert message
     */
    public void sendAlert(String email, String message) {
        sendNotification(email, "Alert", message);
    }
}
