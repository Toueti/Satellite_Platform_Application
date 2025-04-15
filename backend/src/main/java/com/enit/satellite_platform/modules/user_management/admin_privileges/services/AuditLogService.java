package com.enit.satellite_platform.modules.user_management.admin_privileges.services;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);
    // Regex to parse the structured log entry format
    private static final Pattern LOG_ENTRY_PATTERN = Pattern.compile(
            "^\\[(.*?)]\\s+\\[(.*?)]\\s+\\[(.*?)]\\s*(.*)$");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Value("${audit.log.path:/logs}")
    private String auditLogPathString;

    private Path auditLogPath;
    private Path baseLogDir;

    @PostConstruct
    public void initializePaths() {
        try {
            this.baseLogDir = Paths.get(auditLogPathString);
            this.auditLogPath = this.baseLogDir.resolve("audit.log");

            // Optional: Create the directory if it doesn't exist
            if (!Files.exists(this.baseLogDir)) {
                Files.createDirectories(this.baseLogDir);
                logger.info("Created audit log directory: {}", this.baseLogDir.toAbsolutePath());
            }
            logger.info("Audit log path initialized to: {}", auditLogPath.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to create audit log directory: {}", auditLogPathString, e);
            // TODO: Handle the error appropriately, maybe throw a runtime exception
        } catch (Exception e) {
            logger.error("Error initializing audit log paths with value: {}", auditLogPathString, e);
            throw new IllegalStateException("Failed to initialize audit log paths", e);
        }
    }

    /**
     * Retrieves the last N lines from the current audit log file.
     *
     * @param lines          The maximum number of lines to retrieve.
     * @param usernameFilter Optional filter for username (case-insensitive).
     * @param actionFilter   Optional filter for action (case-insensitive).
     * @return A filtered list of log lines, or an empty list if the file cannot be
     *         read.
     */
    public List<String> getLatestAuditLogs(int lines, String usernameFilter, String actionFilter) {
        if (auditLogPath == null || !Files.exists(auditLogPath)) {
            logger.warn("Audit log file not found or path not initialized: {}", auditLogPath);
            return Collections.emptyList();
        }

        List<String> filteredLines = new ArrayList<>();
        try (Stream<String> stream = Files.lines(auditLogPath, StandardCharsets.UTF_8)) {
            // Read all lines first, then filter and take the tail. Inefficient for huge
            // files.
            List<String> allLines = stream.collect(Collectors.toList());

            // Apply filters
            Stream<String> filteredStream = allLines.stream()
                    .filter(line -> matchesFilters(line, usernameFilter, actionFilter));

            // Get the tail of the filtered list
            List<String> tempList = filteredStream.collect(Collectors.toList());
            int start = Math.max(0, tempList.size() - lines);
            filteredLines = tempList.subList(start, tempList.size());

        } catch (IOException e) {
            logger.error("Error reading audit log file: {}", auditLogPath.toAbsolutePath(), e);
            // Return empty list on error
            return Collections.emptyList();
        }
        // Return the filtered list (potentially empty if filtering removed all lines)
        return filteredLines;
    }

    /**
     * Retrieves logs from a specific date's rotated audit log file.
     *
     * @param dateString     The date in 'yyyy-MM-dd' format.
     * @param usernameFilter Optional filter for username (case-insensitive).
     * @param actionFilter   Optional filter for action (case-insensitive).
     * @return A filtered list of log lines from that day's file, or an empty list
     *         if not found/readable.
     */
    public List<String> getAuditLogsByDate(String dateString, String usernameFilter, String actionFilter) {
        if (baseLogDir == null) {
            logger.error("Base log directory path not initialized.");
            return Collections.emptyList();
        }
        // Validate dateString format
        try {
            LocalDate.parse(dateString, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            logger.warn("Invalid date format provided: {}. Expected yyyy-MM-dd.", dateString);
            return Collections.emptyList();
        }

        // Construct the expected filename based on the rolling policy pattern
        String fileName = "audit." + dateString + ".log";
        Path datedLogPath = baseLogDir.resolve(fileName);

        if (!Files.exists(datedLogPath)) {
            logger.warn("Dated audit log file not found: {}", datedLogPath.toAbsolutePath());
            return Collections.emptyList();
        }

        try (Stream<String> stream = Files.lines(datedLogPath, StandardCharsets.UTF_8)) {
            // Read lines, filter, and collect
            return stream
                    .filter(line -> matchesFilters(line, usernameFilter, actionFilter))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Error reading dated audit log file: {}", datedLogPath.toAbsolutePath(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves logs within a specified date range, optionally filtered.
     *
     * @param startDateStr   Start date string (yyyy-MM-dd).
     * @param endDateStr     End date string (yyyy-MM-dd).
     * @param usernameFilter Optional filter for username (case-insensitive).
     * @param actionFilter   Optional filter for action (case-insensitive).
     * @return A list of log lines within the date range, matching filters.
     */
    public List<String> getAuditLogsByDateRange(String startDateStr, String endDateStr, String usernameFilter,
            String actionFilter) {
        LocalDate startDate;
        LocalDate endDate;
        try {
            startDate = LocalDate.parse(startDateStr, DATE_FORMATTER);
            endDate = LocalDate.parse(endDateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            logger.warn("Invalid date format provided for range: {} - {}. Expected yyyy-MM-dd.", startDateStr,
                    endDateStr);
            return Collections.emptyList();
        }

        if (startDate.isAfter(endDate)) {
            logger.warn("Start date {} cannot be after end date {}.", startDateStr, endDateStr);
            return Collections.emptyList();
        }

        List<String> availableDates = listAvailableLogDates();
        List<String> allMatchingLogs = new ArrayList<>();

        // Iterate through dates from start to end
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String dateString = date.format(DATE_FORMATTER);
            // Check if a log file exists for this date (either current or rotated)
            if (date.equals(LocalDate.now())) {
                // Include today's current log file if it exists
                if (Files.exists(auditLogPath)) {
                    allMatchingLogs.addAll(getLogsFromFile(auditLogPath, usernameFilter, actionFilter));
                }
            } else if (availableDates.contains(dateString)) {
                // Include historical log file
                Path datedLogPath = baseLogDir.resolve("audit." + dateString + ".log");
                allMatchingLogs.addAll(getLogsFromFile(datedLogPath, usernameFilter, actionFilter));
            }
        }
        return allMatchingLogs;
    }

    /**
     * Reads the contents of a log file at the given path and returns only the lines
     * that match the given filters.
     * If the file does not exist or cannot be read, an empty list is returned.
     * 
     * @param filePath       The path to a log file.
     * @param usernameFilter Optional filter for username (case-insensitive).
     * @param actionFilter   Optional filter for action (case-insensitive).
     * @return A filtered list of log lines from the file, or an empty list if the
     *         file cannot be read.
     */
    private List<String> getLogsFromFile(Path filePath, String usernameFilter, String actionFilter) {
        if (!Files.exists(filePath)) {
            return Collections.emptyList();
        }
        try (Stream<String> stream = Files.lines(filePath, StandardCharsets.UTF_8)) {
            return stream
                    .filter(line -> matchesFilters(line, usernameFilter, actionFilter))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Error reading audit log file: {}", filePath.toAbsolutePath(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Helper method to check if a log line matches the provided filters.
     * Assumes the log line follows the pattern: [TIMESTAMP] [USER] [ACTION] Details
     *
     * @param line           The log line string.
     * @param usernameFilter Optional username filter (case-insensitive).
     * @param actionFilter   Optional action filter (case-insensitive).
     * @return True if the line matches all active filters, false otherwise.
     */
    private boolean matchesFilters(String line, String usernameFilter, String actionFilter) {
        Matcher matcher = LOG_ENTRY_PATTERN.matcher(line);
        if (!matcher.matches()) {
            // Line doesn't match expected format, include/exclude based on whether filters
            // are active
            return usernameFilter == null && actionFilter == null; // Only include non-matching lines if no filters are
                                                                   // set
        }

        // String timestampStr = matcher.group(1); // Available if needed
        String user = matcher.group(2);
        String action = matcher.group(3);
        // String details = matcher.group(4); // Available if needed

        boolean userMatch = (usernameFilter == null || usernameFilter.isBlank() ||
                (user != null && user.equalsIgnoreCase(usernameFilter)));

        boolean actionMatch = (actionFilter == null || actionFilter.isBlank() ||
                (action != null && action.equalsIgnoreCase(actionFilter)));

        return userMatch && actionMatch;
    }

    /**
     * Lists available historical log files based on the naming pattern.
     *
     * @return A list of date strings (yyyy-MM-dd) for which log files exist.
     */
    public List<String> listAvailableLogDates() {
        if (!Files.isDirectory(baseLogDir)) {
            logger.warn("Log directory not found: {}", baseLogDir.toAbsolutePath());
            return Collections.emptyList();
        }
        try (Stream<Path> stream = Files.list(baseLogDir)) {
            return stream
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.matches("^audit\\.\\d{4}-\\d{2}-\\d{2}\\.log$"))
                    .map(name -> name.substring(6, 16)) // Extract yyyy-MM-dd part
                    .sorted(Collections.reverseOrder()) // Show newest first
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Error listing log directory: {}", baseLogDir.toAbsolutePath(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Writes a structured audit event to the audit log file.
     *
     * @param username The username of the user performing the action (or "SYSTEM"
     *                 if not user-specific).
     * @param action   A short description of the action performed (e.g.,
     *                 "USER_CREATED", "CONFIG_UPDATED").
     * @param details  Additional details about the event (e.g., "User ID: 123",
     *                 "Property: app.feature.enabled=true").
     */
    public void logAuditEvent(String username, String action, String details) {
        if (auditLogPath == null) {
            logger.error("Audit log path is not initialized. Cannot log event: [User: {}, Action: {}, Details: {}]",
                    username, action, details);
            return; // Or throw an exception if logging is critical
        }

        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            // Format: [TIMESTAMP] [USER] [ACTION] Details
            String logEntry = String.format("[%s] [%s] [%s] %s%n",
                    timestamp,
                    username != null ? username : "SYSTEM",
                    action != null ? action : "UNKNOWN_ACTION",
                    details != null ? details : "");

            // Append the log entry to the file, creating it if it doesn't exist.
            Files.writeString(auditLogPath, logEntry, StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND, StandardOpenOption.CREATE);

        } catch (IOException e) {
            logger.error("Failed to write audit event to {}: [User: {}, Action: {}, Details: {}]",
                    auditLogPath.toAbsolutePath(), username, action, details, e);
            // Handle the error appropriately - maybe rethrow, notify, etc.
        } catch (Exception e) {
            // Catch unexpected errors during formatting or writing
            logger.error("Unexpected error writing audit event: [User: {}, Action: {}, Details: {}]",
                    username, action, details, e);
        }
    }
}
