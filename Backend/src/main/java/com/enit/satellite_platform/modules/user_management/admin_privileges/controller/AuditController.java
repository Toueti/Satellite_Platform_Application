package com.enit.satellite_platform.modules.user_management.admin_privileges.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.enit.satellite_platform.modules.user_management.admin_privileges.services.AuditLogService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/audit")
@PreAuthorize("hasRole('ADMIN')") // Ensure only admins can access these endpoints
public class AuditController {

    private final AuditLogService auditLogService;

    public AuditController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * Gets the latest N lines from the current audit log.
     * Defaults to 100 lines if 'lines' parameter is not provided.
     * Allows filtering by username and action.
     */
    @GetMapping("/latest")
    public ResponseEntity<List<String>> getLatestAuditLogs(
            @RequestParam(value = "lines", required = false) Optional<Integer> lines,
            @RequestParam(value = "username", required = false) String usernameFilter,
            @RequestParam(value = "action", required = false) String actionFilter) {
        int numLines = lines.orElse(100); // Default to 100 lines
        List<String> logs = auditLogService.getLatestAuditLogs(numLines, usernameFilter, actionFilter);
        return ResponseEntity.ok(logs);
    }

    /**
     * Gets all logs for a specific date (yyyy-MM-dd).
     * Allows filtering by username and action.
     */
    @GetMapping("/by-date/{dateString}")
    public ResponseEntity<List<String>> getAuditLogsByDate(
            @PathVariable String dateString,
            @RequestParam(value = "username", required = false) String usernameFilter,
            @RequestParam(value = "action", required = false) String actionFilter) {
        // Service layer now handles date format validation
        List<String> logs = auditLogService.getAuditLogsByDate(dateString, usernameFilter, actionFilter);
        // Consider returning 404 if logs list is empty and date was valid but file not found?
        // For now, returning OK with potentially empty list.
        return ResponseEntity.ok(logs);
    }

    /**
     * Gets logs within a specific date range (yyyy-MM-dd).
     * Allows filtering by username and action.
     */
    @GetMapping("/by-range")
    public ResponseEntity<List<String>> getAuditLogsByDateRange(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam(value = "username", required = false) String usernameFilter,
            @RequestParam(value = "action", required = false) String actionFilter) {
        // Service layer handles date format validation and range check
        List<String> logs = auditLogService.getAuditLogsByDateRange(startDate, endDate, usernameFilter, actionFilter);
        return ResponseEntity.ok(logs);
    }

    /**
     * Lists the dates for which historical audit log files are available.
     */
    @GetMapping("/available-dates")
    public ResponseEntity<List<String>> listAvailableLogDates() {
        List<String> dates = auditLogService.listAvailableLogDates();
        return ResponseEntity.ok(dates);
    }

    // Note: The real-time log streaming is handled via WebSocket connection established
    // by the client, not through a specific controller endpoint here. The WebSocketLogAppender
    // pushes logs directly. If more control over the WebSocket (like sending commands)
    // were needed, a @MessageMapping controller method could be added.
}
