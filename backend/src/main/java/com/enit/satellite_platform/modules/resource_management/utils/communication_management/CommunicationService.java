package com.enit.satellite_platform.modules.resource_management.utils.communication_management;

import java.io.File;
import java.util.Map;


/**
 * Generic interface for communication services supporting different input and output types.
 *
 * @param <T> The input type
 * @param <R> The output type
 */
public interface CommunicationService<T, R> {

    /**
     * Send a synchronous request with optional file attachment.
     *
     * @param jsonPart The data part of the request
     * @param filePart Optional file to include (can be null)
     * @param responseType The expected response type class
     * @param authToken Optional authorization token (can be null)
     * @return A wrapper containing the deserialized response object and the saved file part.
     */
    MultipartResponseWrapper<R> sendMultipartRequest(T jsonPart, File filePart, Class<R> responseType, String authToken);

    /**
     * Send a synchronous request with optional file attachment and custom headers.
     *
     * @param jsonPart The data part of the request
     * @param filePart Optional file to include (can be null)
     * @param responseType The expected response type class
     * @param authToken Optional authorization token (can be null)
     * @param headers Optional custom headers (can be null)
     * @return A wrapper containing the deserialized response object and the saved file part.
     */
    default MultipartResponseWrapper<R> sendMultipartRequest(T jsonPart, File filePart, Class<R> responseType, String authToken, Map<String, String> headers) {
        // Default implementation delegates to the basic method
        // Note: Implementations should override this if they support headers differently
        return sendMultipartRequest(jsonPart, filePart, responseType, authToken);
    }

    /**
     * Send data asynchronously.
     *
     * @param jsonPart The data part of the request
     * @param filePart Optional file to include (can be null)
     */
    void sendAsync(T jsonPart, File filePart);

    /**
     * Send data asynchronously with custom headers.
     *
     * @param jsonPart The data part of the request
     * @param filePart Optional file to include (can be null)
     * @param headers Optional custom headers (can be null)
     */
    default void sendAsync(T jsonPart, File filePart, Map<String, String> headers) {
        // Default implementation delegates to the basic method
        sendAsync(jsonPart, filePart);
    }
}
