package com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enit.satellite_platform.modules.resource_management.utils.communication_management.CommunicationService;
import com.enit.satellite_platform.modules.resource_management.utils.communication_management.MultipartResponseWrapper; // Added import
import com.enit.satellite_platform.modules.resource_management.utils.serialization.Serializer;
import com.enit.satellite_platform.modules.resource_management.utils.serialization.SerializerFactory;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitMqCommunicationService<T, R> implements CommunicationService<T, R> {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMqCommunicationService.class);

    private final Map<String, String> config;
    private final Connection connection;
    private final Channel channel;
    private final String requestQueueName;
    private final SerializerFactory serializerFactory;

    public RabbitMqCommunicationService(Map<String, String> config, SerializerFactory serializerFactory) {
        this.config = config;
        this.serializerFactory = serializerFactory;
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(this.config.getOrDefault("rabbitmq.host", "localhost"));
            factory.setPort(Integer.parseInt(this.config.getOrDefault("rabbitmq.port", "5672")));
            factory.setUsername(this.config.getOrDefault("rabbitmq.user", "guest"));
            factory.setPassword(this.config.getOrDefault("rabbitmq.pass", "guest"));
            this.connection = factory.newConnection();
            this.channel = connection.createChannel();
            this.requestQueueName = this.config.getOrDefault("rabbitmq.requestQueue", "geo_request_queue");
            channel.queueDeclare(requestQueueName, true, false, false, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize RabbitMQ connection", e);
        }
    }

    // Cleanup method (call this when shutting down)
    public void shutdown() {
        try {
            if (channel != null && channel.isOpen()) channel.close();
            if (connection != null && connection.isOpen()) connection.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to close RabbitMQ resources", e);
        }
    }

    @Override
    public MultipartResponseWrapper<R> sendMultipartRequest(T jsonPart, File filePart, Class<R> responseType, String authToken) {
        // Synchronous request-response is not supported for RabbitMQ in this setup
        throw new UnsupportedOperationException("Synchronous communication is not supported via RabbitMQ in this implementation.");
    }

    @Override
    public void sendAsync(T jsonPart, File filePart) {
        sendAsync(jsonPart, filePart, new HashMap<>());
    }

    @Override
    public void sendAsync(T jsonPart, File filePart, Map<String, String> headers) {
        logger.debug("Attempting to send data asynchronously via RabbitMQ. Queue: {}", requestQueueName);

        // Choose serializer based on content type header or default to JSON
        String contentType = headers != null ? headers.get("Content-Type") : null;
        Serializer serializer = serializerFactory.getSerializerByContentType(contentType);

        // Create a Map to hold the message data
        Map<String, Object> messageData = new HashMap<>();
        
        // Add JSON part if exists
        if (jsonPart != null) {
            String serialized = serializer.serialize(jsonPart);
            messageData.put("payload", serialized);
            messageData.put("content_type", serializer.getContentType());
        }
        
        // Add authentication token if provided
        if (headers != null && headers.containsKey("Authorization")) {
            messageData.put("authorization", headers.get("Authorization"));
        } else if (headers != null && headers.containsKey("auth_token")) {
            messageData.put("authorization", headers.get("auth_token"));
        }
        
        // Add custom headers if any
        if (headers != null && !headers.isEmpty()) {
            messageData.put("headers", headers);
        }

        // Read file content and add to payload if file exists
        if (filePart != null && filePart.exists()) {
            try {
                byte[] fileBytes = Files.readAllBytes(filePart.toPath());
                messageData.put("file_content", fileBytes);
                messageData.put("file_name", filePart.getName());
                
                // Try to determine MIME type
                try {
                    String mimeType = Files.probeContentType(filePart.toPath());
                    messageData.put("file_content_type", mimeType != null ? mimeType : "application/octet-stream");
                } catch (IOException e) {
                    logger.warn("Could not determine MIME type for file {}: {}", filePart.getName(), e.getMessage());
                    messageData.put("file_content_type", "application/octet-stream");
                }
                
                logger.debug("Added file '{}' ({} bytes) to payload.", filePart.getName(), fileBytes.length);
            } catch (IOException e) {
                logger.error("Failed to read file part: {}", filePart.getAbsolutePath(), e);
                throw new RuntimeException("Failed to read file part for async sending: " + filePart.getName(), e);
            }
        } else {
            logger.debug("No file part provided or file does not exist.");
        }

        // Serialize the final message data
        byte[] messageBytes;
        try {
            String serializedMessage = serializer.serialize(messageData);
            messageBytes = serializedMessage.getBytes();
            logger.debug("Serialized payload size: {} bytes", messageBytes.length);
        } catch (Exception e) {
            logger.error("Failed to serialize payload for RabbitMQ", e);
            throw new RuntimeException("Failed to serialize payload", e);
        }

        // Prepare message properties
        AMQP.BasicProperties.Builder propertiesBuilder = new AMQP.BasicProperties.Builder()
            .contentType(serializer.getContentType())
            .deliveryMode(2); // persistent message
        
        // Add custom headers as message properties if any
        if (headers != null && !headers.isEmpty()) {
            Map<String, Object> headerMap = new HashMap<>(headers);
            propertiesBuilder.headers(headerMap);
        }
        
        AMQP.BasicProperties properties = propertiesBuilder.build();

        // Publish the message
        try {
            channel.basicPublish("", requestQueueName, properties, messageBytes);
            logger.info("Successfully published message of size {} bytes to queue '{}'", messageBytes.length, requestQueueName);
        } catch (IOException e) {
            logger.error("Failed to publish message to RabbitMQ queue '{}'", requestQueueName, e);
            throw new RuntimeException("Failed to publish message to RabbitMQ", e);
        }
    }
}
