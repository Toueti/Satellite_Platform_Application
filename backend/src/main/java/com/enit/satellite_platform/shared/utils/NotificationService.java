package com.enit.satellite_platform.shared.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    // Use a specific logger or marker for alerts
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final Marker ALERT_MARKER = MarkerFactory.getMarker("ALERT");

    /**
     * Sends an alert notification, currently by logging with a specific marker.
     * This could be extended to send emails, push notifications, etc.
     *
     * @param subject A short title or subject for the alert.
     * @param details Detailed information about the alert event.
     */
    public void sendAlert(String subject, String details) {
        // Log using the ALERT marker for easy filtering in log aggregation tools
        logger.error(ALERT_MARKER, "ALERT Subject: {} - Details: {}", subject, details);

        // Future enhancements could go here:
        // - Send email notification if email service is configured
        // - Push to a message queue (Kafka, RabbitMQ)
        // - Call an external alerting service API
    }

    /**
     * Overloaded method for convenience when only details are needed.
     *
     * @param details Detailed information about the alert event.
     */
     public void sendAlert(String details) {
        sendAlert("System Alert", details);
     }
}
