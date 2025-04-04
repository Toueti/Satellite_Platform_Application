package com.enit.satellite_platform.shared.dto;

import java.time.LocalDateTime;

/**
 * A generic response class to encapsulate API responses.
 *
 * @param <T> The type of the data contained in the response.
 */
public class GenericResponse<T> {

    /**
     * The timestamp when the response was created.
     */
    private LocalDateTime timestamp;
    /**
     * The status of the response (e.g., "SUCCESS", "FAILURE").
     */
    private String status;
    /**
     * A message providing additional information about the response.
     */
    private String message;
    /**
     * The data payload of the response.
     */
    private T data;

    /**
     * Constructs a GenericResponse with a status, message, and data.
     *
     * @param status  The status of the response.
     * @param message The message associated with the response.
     * @param data    The data payload of the response.
     */
    public GenericResponse(String status, String message, T data) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.message = message;
        this.data = data;
    }

    /**
     * Constructs a GenericResponse with a status and message, without data.
     *
     * @param status The status of the response.
     * @param message The message associated with the response.
     */
    public GenericResponse(String status, String message) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.message = message;

    }

    // Getters and setters

    /**
     * Gets the timestamp of the response.
     *
     * @return The timestamp.
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp of the response.
     *
     * @param timestamp The timestamp to set.
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the status of the response.
     *
     * @return The status.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status of the response.
     *
     * @param status The status to set.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the message associated with the response.
     *
     * @return The message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message associated with the response.
     *
     * @param message The message to set.
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets the data payload of the response.
     *
     * @return The data.
     */
    public T getData() {
        return data;
    }

    /**
     * Sets the data payload of the response.
     *
     * @param data The data to set.
     */
    public void setData(T data) {
        this.data = data;
    }
}
