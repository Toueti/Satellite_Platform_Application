package com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.enit.satellite_platform.modules.resource_management.utils.communication_management.CommunicationPayload;
import com.enit.satellite_platform.modules.resource_management.utils.communication_management.CommunicationService;
import com.enit.satellite_platform.modules.resource_management.utils.communication_management.MultipartResponseWrapper; // Added import
import com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc.GeoSpatialServiceGrpc;
import com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc.GeoSpatialServiceGrpc.GeoSpatialServiceBlockingStub;
import com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc.GeoSpatialServiceGrpc.GeoSpatialServiceStub;
import com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc.PayloadRequest;
import com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc.PayloadResponse;
import com.google.protobuf.ByteString;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

@Service // Make it a Spring component
public class GrpcCommunicationService implements CommunicationService<CommunicationPayload, CommunicationPayload> {

    private static final Logger logger = LoggerFactory.getLogger(GrpcCommunicationService.class);

    // --- Configuration Properties ---
    @Value("${grpc.client.address:localhost}")
    private String host;

    @Value("${grpc.client.port:50051}")
    private int port;

    @Value("${grpc.client.max-message_size_mb:100}")
    private int maxMessageSizeMB; // Max message size in MB

    @Value("${grpc.client.use_transport_security:false}")
    private boolean useTransportSecurity;

    @Value("${grpc.client.shutdown_timeout_seconds:5}")
    private int shutdownTimeoutSeconds;

    // --- gRPC Components ---
    private ManagedChannel channel;
    private GeoSpatialServiceBlockingStub blockingStub;
    private GeoSpatialServiceStub asyncStub;

    // --- Initialization ---
    @PostConstruct // Initialize after dependency injection
    public void init() {
        logger.info("Initializing gRPC channel to {}:{}", host, port);
        int maxSizeBytes = maxMessageSizeMB * 1024 * 1024; // Convert MB to Bytes

        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forAddress(host, port)
                .maxInboundMessageSize(maxSizeBytes); // Configure max message size

        if (useTransportSecurity) {
            logger.info("Using transport security (TLS/SSL) for gRPC channel.");
            channelBuilder.useTransportSecurity(); // Use TLS/SSL
        } else {
            logger.warn("Using plaintext for gRPC channel. NOT recommended for production.");
            channelBuilder.usePlaintext(); // Use plaintext (for development)
        }

        this.channel = channelBuilder.build();
        this.blockingStub = GeoSpatialServiceGrpc.newBlockingStub(channel);
        this.asyncStub = GeoSpatialServiceGrpc.newStub(channel); // Initialize async stub
    }

    // --- Core Send/Receive Logic ---
    public CommunicationPayload sendAndReceiveData(CommunicationPayload payload) {
        return sendAndReceiveDataInternal(payload, null); // No auth token by default
    }

    private CommunicationPayload sendAndReceiveDataInternal(CommunicationPayload payload, String authToken) {
        Objects.requireNonNull(payload, "CommunicationPayload cannot be null");
        logger.debug("Sending data via gRPC. Payload keys: {}", payload.getAllData().keySet());

        PayloadRequest request = buildGrpcRequest(payload);
        GeoSpatialServiceBlockingStub currentStub = getAuthenticatedStub(authToken);

        try {
            logger.debug("Executing blocking gRPC call...");
            PayloadResponse grpcResponse = currentStub.processData(request);
            logger.debug("Received gRPC response.");
            return convertGrpcResponse(grpcResponse);
        } catch (StatusRuntimeException e) {
            logger.error("gRPC call failed with status: {} - {}", e.getStatus().getCode(), e.getStatus().getDescription(), e);
            // Consider mapping gRPC status codes to custom application exceptions
            throw new RuntimeException("gRPC communication failed: " + e.getStatus(), e);
        } catch (Exception e) {
            logger.error("An unexpected error occurred during gRPC communication", e);
            throw new RuntimeException("Unexpected gRPC error", e);
        }
    }

    // --- Multipart Request Handling ---
    @Override
    public MultipartResponseWrapper<CommunicationPayload> sendMultipartRequest(CommunicationPayload jsonPart, File filePart,
            Class<CommunicationPayload> responseType, String authToken) {
        Objects.requireNonNull(jsonPart, "JSON part cannot be null");
        logger.info("Sending pseudo-multipart request via gRPC with auth token (present: {})", authToken != null && !authToken.isBlank());

        CommunicationPayload combinedPayload = new CommunicationPayload();

        // Copy data from jsonPart
        jsonPart.getAllData().forEach((key, value) -> {
            combinedPayload.addData(key, value);
            String contentType = jsonPart.getContentType(key);
            if (contentType != null) {
                combinedPayload.setContentType(key, contentType);
            }
        });

        // Read file content and add to payload
        if (filePart != null && filePart.exists()) {
            try {
                byte[] fileBytes = Files.readAllBytes(filePart.toPath());
                String fileKey = "file"; // Consider making this configurable or dynamic
                combinedPayload.addData(fileKey, fileBytes);
                // Try to determine MIME type, fallback to octet-stream
                String mimeType = Files.probeContentType(filePart.toPath());
                combinedPayload.setContentType(fileKey, mimeType != null ? mimeType : "application/octet-stream");
                logger.debug("Added file '{}' with size {} bytes and type {}", filePart.getName(), fileBytes.length, combinedPayload.getContentType(fileKey));
            } catch (IOException e) {
                logger.error("Failed to read file part: {}", filePart.getAbsolutePath(), e);
                throw new RuntimeException("Failed to read file part: " + filePart.getName(), e);
            }
        } else if (filePart != null) {
             logger.warn("File part provided but does not exist or is not accessible: {}", filePart.getAbsolutePath());
        }

        // Send via gRPC using the internal method with authentication
        CommunicationPayload resultPayload = sendAndReceiveDataInternal(combinedPayload, authToken);

        // Wrap the result. Since gRPC response here doesn't separate files, savedFile is null.
        return new MultipartResponseWrapper<>(resultPayload, null, null, null);
    }

    // --- Asynchronous Request Handling ---
    @Override
    public void sendAsync(CommunicationPayload jsonPart, File filePart) {
        Objects.requireNonNull(jsonPart, "JSON part cannot be null for async request");
        logger.info("Sending asynchronous pseudo-multipart request via gRPC");

        CommunicationPayload combinedPayload = new CommunicationPayload();
        jsonPart.getAllData().forEach((key, value) -> {
            combinedPayload.addData(key, value);
            String contentType = jsonPart.getContentType(key);
            if (contentType != null) combinedPayload.setContentType(key, contentType);
        });

        if (filePart != null && filePart.exists()) {
            try {
                byte[] fileBytes = Files.readAllBytes(filePart.toPath());
                String fileKey = "file";
                combinedPayload.addData(fileKey, fileBytes);
                String mimeType = Files.probeContentType(filePart.toPath());
                combinedPayload.setContentType(fileKey, mimeType != null ? mimeType : "application/octet-stream");
            } catch (IOException e) {
                 logger.error("Failed to read file part for async request: {}", filePart.getAbsolutePath(), e);
                 // Decide how to handle this - maybe log and continue without file? Or fail?
                 // For now, log and proceed without file.
            }
        }

        PayloadRequest request = buildGrpcRequest(combinedPayload);

        // Note: Async calls don't easily support request-scoped authentication like blocking calls
        // If auth is needed, it might require a different approach (e.g., CallCredentials)
        GeoSpatialServiceStub currentAsyncStub = this.asyncStub; // Use the base async stub

        logger.debug("Executing asynchronous gRPC call...");
        currentAsyncStub.processData(request, new StreamObserver<PayloadResponse>() {
            @Override
            public void onNext(PayloadResponse response) {
                logger.info("Received async gRPC response.");
                // Process the response if needed (e.g., update status, notify user)
                // CommunicationPayload result = convertGrpcResponse(response);
                // logger.debug("Async response processed: {}", result.getAllData().keySet());
            }

            @Override
            public void onError(Throwable t) {
                if (t instanceof StatusRuntimeException) {
                    StatusRuntimeException e = (StatusRuntimeException) t;
                    logger.error("Async gRPC call failed with status: {} - {}", e.getStatus().getCode(), e.getStatus().getDescription(), e);
                } else {
                    logger.error("Async gRPC call failed with unexpected error", t);
                }
                // Handle the error (e.g., retry logic, notify monitoring system)
            }

            @Override
            public void onCompleted() {
                logger.info("Async gRPC call completed.");
                // Perform any cleanup or final actions
            }
        });
    }


    // --- Helper Methods ---

    private PayloadRequest buildGrpcRequest(CommunicationPayload payload) {
        PayloadRequest.Builder requestBuilder = PayloadRequest.newBuilder();
        for (Map.Entry<String, Object> entry : payload.getAllData().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String contentType = payload.getContentType(key); // Get content type

            // Skip internal content type markers
            if (key.endsWith("_content_type")) continue;

            if (value instanceof byte[]) {
                requestBuilder.putData(key, ByteString.copyFrom((byte[]) value));
                if (contentType != null) requestBuilder.putMetadata(key + "_content_type", contentType);
                logger.trace("Added byte data for key '{}', content-type: {}", key, contentType);
            } else if (value instanceof String) {
                requestBuilder.putData(key, ByteString.copyFromUtf8((String) value));
                 // Default content type for string is text/plain if not specified
                String effectiveContentType = (contentType != null) ? contentType : "text/plain";
                requestBuilder.putMetadata(key + "_content_type", effectiveContentType);
                logger.trace("Added string data for key '{}', content-type: {}", key, effectiveContentType);
            } else if (value != null) {
                 logger.warn("Unsupported data type for key '{}': {}", key, value.getClass().getName());
                 // Optionally convert to string or handle differently
                 requestBuilder.putData(key, ByteString.copyFromUtf8(value.toString()));
                 requestBuilder.putMetadata(key + "_content_type", "text/plain");
            } else {
                 logger.warn("Null value encountered for key '{}'", key);
                 // Handle null value if necessary, e.g., skip or send an empty ByteString
                 continue; // Skip null values
                 // Or if you want to send an empty ByteString, uncomment the next line:
                 // requestBuilder.putData(key, ByteString.EMPTY);
                 // requestBuilder.putMetadata(key + "_content_type", "application/octet-stream"); // Or appropriate type
            }
        }
        return requestBuilder.build();
    }

    private CommunicationPayload convertGrpcResponse(PayloadResponse grpcResponse) {
        CommunicationPayload response = new CommunicationPayload();
        if (grpcResponse == null) {
             logger.warn("Received null gRPC response.");
             return response; // Return empty payload
        }

        for (Map.Entry<String, ByteString> entry : grpcResponse.getDataMap().entrySet()) {
            String key = entry.getKey();
            ByteString byteString = entry.getValue();
            if (byteString == null) {
                 logger.warn("Received null ByteString for key '{}' in gRPC response.", key);
                 continue; // Skip null data entries
            }
            byte[] data = byteString.toByteArray();

            // Use getOrDefault for safer metadata access
            String contentType = grpcResponse.getMetadataMap().getOrDefault(key + "_content_type", "application/octet-stream"); // Default if missing

            // Basic content type handling (can be expanded)
            if (contentType.startsWith("image/")) {
                response.addFile(key, data); // Treat as file/binary
            } else if (contentType.startsWith("text/")) {
                try {
                    response.addText(key, new String(data, "UTF-8")); // Assume UTF-8 for text
                } catch (Exception e) {
                     logger.warn("Failed to decode text data for key '{}' as UTF-8, storing as bytes.", key, e);
                     response.addFile(key, data); // Fallback to storing as bytes
                }
            } else {
                 logger.debug("Received data with content type '{}' for key '{}', storing as raw bytes.", contentType, key);
                 response.addFile(key, data); // Store other types as raw bytes
            }
            response.setContentType(key, contentType); // Always store the content type
        }
        logger.debug("Converted gRPC response to CommunicationPayload. Keys: {}", response.getAllData().keySet());
        return response;
    }

    private GeoSpatialServiceBlockingStub getAuthenticatedStub(String authToken) {
        if (authToken != null && !authToken.isBlank()) {
            //Metadata headers = new Metadata();
            Metadata.Key<String> authKey = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
            logger.debug("Attaching Authorization header via ClientInterceptor.");

            ClientInterceptor interceptor = new ClientInterceptor() {
                @Override
                public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                        MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
                    return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
                        @Override
                        public void start(Listener<RespT> responseListener, Metadata headers) {
                            headers.put(authKey, "Bearer " + authToken);
                            super.start(responseListener, headers);
                        }
                    };
                }
            };
            return this.blockingStub.withInterceptors(interceptor);
        }
        return this.blockingStub; // Return the base stub if no token
    }


    // --- Cleanup ---
    @PreDestroy // Ensure channel is shut down when the bean is destroyed
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            logger.info("Shutting down gRPC channel to {}:{}", host, port);
            try {
                channel.shutdown().awaitTermination(shutdownTimeoutSeconds, TimeUnit.SECONDS);
                logger.info("gRPC channel shutdown complete.");
            } catch (InterruptedException e) {
                logger.warn("gRPC channel shutdown interrupted. Forcing shutdown.", e);
                channel.shutdownNow(); // Force shutdown if interrupted
                Thread.currentThread().interrupt(); // Preserve interrupt status
            }
        } else if (channel != null) {
             logger.info("gRPC channel to {}:{} was already shut down.", host, port);
        } else {
             logger.warn("gRPC channel was null, cannot shut down.");
        }
    }
}
