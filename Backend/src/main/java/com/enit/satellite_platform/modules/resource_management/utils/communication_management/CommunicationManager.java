package com.enit.satellite_platform.modules.resource_management.utils.communication_management;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;


/**
 * Facade for selecting and using the appropriate communication service
 * based on requirements like file size and synchronicity.
 * Manages interaction with different communication service implementations.
 */
@Service
public class CommunicationManager {

    private static final Logger logger = LoggerFactory.getLogger(CommunicationManager.class);
    private static final long FILE_SIZE_THRESHOLD_BYTES = 10 * 1024 * 1024; // 10MB

    @Value("${external.app.endpoints.image_processor}")
    private String imageProcessorEndpoint;

    @Value("${spring.rabbitmq.host}")
    private String rabbitMqHost;
    @Value("${spring.rabbitmq.port}")
    private String rabbitMqPort;
    @Value("${spring.rabbitmq.username}")
    private String rabbitMqUser;
    @Value("${spring.rabbitmq.password}")
    private String rabbitMqPass;

    private final CommunicationServiceFactory communicationServiceFactory;

    public CommunicationManager(CommunicationServiceFactory communicationServiceFactory) {
        this.communicationServiceFactory = communicationServiceFactory;
    }

    /**
     * Sends data using the appropriate communication method based on parameters.
     *
     * @param <T>          Type of input data
     * @param <R>          Type of expected response
     * @param jsonPart     The metadata or JSON part of the payload
     * @param filePart     The file part of the payload (can be null)
     * @param isAsync      True for asynchronous communication (RabbitMQ), false for
     *                     synchronous (REST/gRPC)
     * @param responseType The expected response type for synchronous calls
     * @param authToken    Optional authentication token (e.g., JWT) for secured
     *                     endpoints
     * @param headers      Optional custom headers for the request
     * @return A wrapper containing the response payload and potentially a saved file for synchronous calls,
     *         null for asynchronous calls.
     * @throws RuntimeException if communication fails
     */
    public <T, R> MultipartResponseWrapper<R> sendData(T jsonPart, File filePart, boolean isAsync, Class<R> responseType,
            String authToken, Map<String, String> headers) {
        CommunicationTechnology technology;
        long fileSize = (filePart != null && filePart.exists()) ? filePart.length() : 0;

        if (isAsync) {
            technology = CommunicationTechnology.RABBITMQ;
            logger.debug("Selecting RabbitMQ for asynchronous communication.");
        } else {
            if (fileSize >= FILE_SIZE_THRESHOLD_BYTES) {
                technology = CommunicationTechnology.GRPC;
                logger.debug("Selecting gRPC for synchronous communication (file size {} >= {} bytes).",
                        fileSize, FILE_SIZE_THRESHOLD_BYTES);
            } else {
                technology = CommunicationTechnology.REST_API;
                logger.debug("Selecting REST API for synchronous communication (file size {} < {} bytes).",
                        fileSize, FILE_SIZE_THRESHOLD_BYTES);
            }
        }

        // Create a config map specifically for the selected technology
        Map<String, String> serviceConfig = createConfigForTechnology(technology);

        // Get the appropriate service instance
        CommunicationService<T, R> service = communicationServiceFactory.createService(technology, serviceConfig);

        try {
            if (isAsync) {
                // Asynchronous call (fire-and-forget)
                service.sendAsync(jsonPart, filePart, headers != null ? headers : Collections.emptyMap());
                return null; // No response wrapper expected for async calls
            } else {
                // Synchronous call - Pass token and headers along, expect wrapper back
                return service.sendMultipartRequest(jsonPart, filePart, responseType, authToken,
                        headers != null ? headers : Collections.emptyMap());
            }
        } catch (UnsupportedOperationException uoe) {
            logger.error("Attempted an unsupported operation for technology {}: Async={}, Sync={}",
                    technology, isAsync, !isAsync, uoe);
            throw uoe; // Re-throw specific exception
        } catch (Exception e) {
            logger.error("Error during communication via {}: {}", technology, e.getMessage(), e);
            // Wrap other exceptions
            throw new RuntimeException("Communication failed via " + technology, e);
        }
    }

    /**
     * Simplified version of sendData that uses defaults for headers.
     * Returns a wrapper for synchronous calls.
     */
    public <T, R> MultipartResponseWrapper<R> sendData(T jsonPart, File filePart, boolean isAsync, Class<R> responseType, String authToken) {
        return sendData(jsonPart, filePart, isAsync, responseType, authToken, Collections.emptyMap());
    }

    /**
     * Sends data asynchronously using RabbitMQ.
     *
     * @param <T>       Type of input data
     * @param jsonPart  The metadata or JSON part of the payload
     * @param filePart  The file part of the payload (can be null)
     * @param authToken Optional authentication token (e.g., JWT) if needed by the
     *                  async receiver
     * @param headers   Optional custom headers for the request
     */
    public <T> void sendDataAsync(T jsonPart, File filePart, String authToken, Map<String, String> headers) {
        CommunicationTechnology technology = CommunicationTechnology.RABBITMQ;
        logger.debug("Selecting RabbitMQ for explicit asynchronous communication.");

        // Create a config map specifically for RabbitMQ
        Map<String, String> rabbitMqConfig = createConfigForTechnology(technology);

        // Add auth token to headers if provided
        Map<String, String> finalHeaders = new HashMap<>(headers != null ? headers : Collections.emptyMap());
        if (authToken != null && !authToken.isBlank()) {
            finalHeaders.put("Authorization", "Bearer " + authToken);
        }

        // Get the RabbitMQ service instance
        CommunicationService<T, ?> service = communicationServiceFactory.createService(technology, rabbitMqConfig);

        try {
            service.sendAsync(jsonPart, filePart, finalHeaders);
        } catch (UnsupportedOperationException uoe) {
            logger.error("RabbitMQ service unexpectedly does not support sendAsync", uoe);
            throw uoe; // Should not happen based on our implementation
        } catch (Exception e) {
            logger.error("Error during asynchronous communication via {}: {}", technology, e.getMessage(), e);
            throw new RuntimeException("Asynchronous communication failed via " + technology, e);
        }
    }

    /**
     * Simplified version of sendDataAsync that uses defaults for headers
     */
    public <T> void sendDataAsync(T jsonPart, File filePart, String authToken) {
        sendDataAsync(jsonPart, filePart, authToken, Collections.emptyMap());
    }

    /**
     * Sends data synchronously using REST or gRPC based on file size.
     *
     * @param <T>          Type of input data
     * @param <R>          Type of expected response
     * @param jsonPart     The metadata or JSON part of the payload
     * @param filePart     The file part of the payload (can be null)
     * @param responseType The expected response type
     * @param authToken    Optional authentication token (e.g., JWT) for secured
     *                     endpoints
     * @param headers      Optional custom headers for the request
     * @return A wrapper containing the response payload and potentially a saved file.
     */
    public <T, R> MultipartResponseWrapper<R> sendDataSync(T jsonPart, File filePart, Class<R> responseType,
            String authToken, Map<String, String> headers) {
        CommunicationTechnology technology;
        long fileSize = (filePart != null && filePart.exists()) ? filePart.length() : 0;

        if (fileSize >= FILE_SIZE_THRESHOLD_BYTES) {
            technology = CommunicationTechnology.GRPC;
            logger.debug("Selecting gRPC for explicit synchronous communication (file size {} >= {} bytes).",
                    fileSize, FILE_SIZE_THRESHOLD_BYTES);
        } else {
            technology = CommunicationTechnology.REST_API;
            logger.debug("Selecting REST API for explicit synchronous communication (file size {} < {} bytes).",
                    fileSize, FILE_SIZE_THRESHOLD_BYTES);
        }

        // Create a config map specifically for the selected technology
        Map<String, String> serviceConfig = createConfigForTechnology(technology);

        // Get the appropriate service instance
        CommunicationService<T, R> service = communicationServiceFactory.createService(technology, serviceConfig);

        try {
            // Call the synchronous method directly, passing the token and headers
            return service.sendMultipartRequest(jsonPart, filePart, responseType, authToken,
                    headers != null ? headers : Collections.emptyMap());
        } catch (UnsupportedOperationException uoe) {
            logger.error("Selected synchronous service {} unexpectedly does not support sendMultipartRequest",
                    technology, uoe);
            throw uoe; // Should not happen based on our implementation
        } catch (Exception e) {
            logger.error("Error during synchronous communication via {}: {}", technology, e.getMessage(), e);
            throw new RuntimeException("Synchronous communication failed via " + technology, e);
        }
    }

    /**
     * Simplified version of sendDataSync that uses defaults for headers.
     * Returns a wrapper.
     */
    public <T, R> MultipartResponseWrapper<R> sendDataSync(T jsonPart, File filePart, Class<R> responseType, String authToken) {
        return sendDataSync(jsonPart, filePart, responseType, authToken, Collections.emptyMap());
    }

    /**
     * Create a configuration map for a specific communication technology.
     * This can be expanded to pull from application properties or other sources.
     * 
     * @param technology The communication technology
     * @return A map with configuration values
     */
    private Map<String, String> createConfigForTechnology(CommunicationTechnology technology) {
        Map<String, String> config = new HashMap<>();

        switch (technology) {
            case RABBITMQ:
                // Example RabbitMQ config
                config.put("rabbitmq.host", rabbitMqHost);
                config.put("rabbitmq.port", rabbitMqPort);
                config.put("rabbitmq.user", rabbitMqUser);
                config.put("rabbitmq.pass", rabbitMqPass);
                config.put("rabbitmq.requestQueue", "geo_request_queue");
            case REST_API:
                config.put("rest.endpoint", imageProcessorEndpoint);
                break;
            case GRPC:
                config.put("grpc.host", "localhost");
                config.put("grpc.port", "9090");
                break;
            default:
                // No specific config needed
        }

        return config;
    }
}
