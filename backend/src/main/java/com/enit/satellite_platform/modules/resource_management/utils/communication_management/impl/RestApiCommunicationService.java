package com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.io.ByteArrayInputStream; // Added for MultipartFile wrapper
import java.io.InputStream; // Added for MultipartFile wrapper

import org.slf4j.Logger;
import java.time.Duration; // Added for Retry
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull; // Added for MultipartFile wrapper
import org.springframework.util.Assert; // Added for MultipartFile wrapper
import org.springframework.web.multipart.MultipartFile; // Added for MultipartFile wrapper
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException; // Added for Retry
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry; // Added for Retry

import com.enit.satellite_platform.modules.resource_management.utils.communication_management.CommunicationService;
import com.enit.satellite_platform.modules.resource_management.utils.communication_management.MultipartResponseWrapper;
import com.enit.satellite_platform.modules.resource_management.utils.serialization.Serializer;
import com.enit.satellite_platform.modules.resource_management.utils.serialization.SerializerFactory;
import com.enit.satellite_platform.modules.resource_management.utils.storage_management.StorageManager; // Added import

public class RestApiCommunicationService<T, R> implements CommunicationService<T, R> {
    private static final Logger logger = LoggerFactory.getLogger(RestApiCommunicationService.class);
    // TEMP_FILE_PREFIX removed as it's no longer directly used for temp file
    // creation here
    private static final String BINARY_CONTENT_TYPE_PREFIX = "application/octet-stream,image/,video/,audio/";

    private final String endpoint;
    private final SerializerFactory serializerFactory;
    private final WebClient webClient;
    private final StorageManager storageManager; // Added StorageManager

    public RestApiCommunicationService(
            String endpoint, // Endpoint is passed by the factory from config
            SerializerFactory serializerFactory,
            WebClient webClient,
            StorageManager storageManager) { // Inject StorageManager
        this.serializerFactory = serializerFactory;
        if (endpoint == null || !endpoint.startsWith("http")) {
            throw new IllegalArgumentException("Invalid endpoint URL: " + endpoint);
        }
        this.endpoint = endpoint; // Consider making final if possible
        this.webClient = webClient.mutate().baseUrl(this.endpoint).build();
        this.storageManager = storageManager; // Assign injected manager
        logger.info("RestApiCommunicationService initialized with endpoint: {}", this.endpoint);
    }

    // Inner class to wrap byte[] as MultipartFile for StorageManager
    private static class ByteArrayMultipartFile implements MultipartFile {
        private final byte[] content;
        private final String name;
        private final String originalFilename;
        private final String contentType;

        public ByteArrayMultipartFile(byte[] content, String name, String originalFilename, String contentType) {
            Assert.notNull(content, "Content must not be null");
            this.content = content;
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
        }

        @Override
        @NonNull
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        @NonNull
        public byte[] getBytes() throws IOException {
            return content;
        }

        @Override
        @NonNull
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(@NonNull File dest) throws IOException, IllegalStateException {
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(content);
            }
        }
    }

    private boolean isBinaryContentType(String contentType) {
        if (contentType == null)
            return false;
        return BINARY_CONTENT_TYPE_PREFIX.contains(contentType.toLowerCase());
    }

    private String extractFilename(HttpHeaders headers) {
        String contentDisposition = headers.getFirst(HttpHeaders.CONTENT_DISPOSITION);
        if (contentDisposition != null && contentDisposition.contains("filename=")) {
            // More robust extraction to handle quotes and potential variations
            String[] parts = contentDisposition.split(";");
            for (String part : parts) {
                part = part.trim();
                if (part.startsWith("filename=")) {
                    String filename = part.substring("filename=".length());
                    // Remove surrounding quotes if present
                    if (filename.startsWith("\"") && filename.endsWith("\"")) {
                        filename = filename.substring(1, filename.length() - 1);
                    }
                    return filename;
                }
            }
        }
        // Fallback if filename not found in Content-Disposition: generate a UUID name
        MediaType contentType = headers.getContentType();
        String extension = ".tmp"; // Default extension
        if (contentType != null && contentType.getSubtype() != null && !contentType.getSubtype().isEmpty()) {
            extension = "." + contentType.getSubtype(); // Use subtype as extension if available
        }
        // Return a UUID-based name without the prefix
        return UUID.randomUUID().toString() + extension;
    }

    @Override
    public MultipartResponseWrapper<R> sendMultipartRequest(T jsonPart, File filePart, Class<R> responseType,
            String authToken) {
        return sendMultipartRequest(jsonPart, filePart, responseType, authToken, Collections.emptyMap());
    }

    @Override
    public MultipartResponseWrapper<R> sendMultipartRequest(T jsonPart, File filePart, Class<R> responseType,
            String authToken,
            Map<String, String> customHeaders) {

        // Basic Input Validation
        if (responseType == null) {
            throw new IllegalArgumentException("responseType cannot be null");
        }
        // Note: jsonPart and filePart nullability might be valid depending on use case

        String fullUrl = buildFullUrl(customHeaders); // Use helper method
        logger.debug("Preparing multipart request to {}", fullUrl);

        try {
            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            Serializer serializer = getSerializer(customHeaders); // Use helper method

            if (jsonPart != null) {
                String json = serializer.serialize(jsonPart);
                bodyBuilder.part("metadata", json).header(HttpHeaders.CONTENT_TYPE, serializer.getContentType());
                logger.debug("Added JSON part ({} bytes)", json.getBytes().length);
            }

            if (filePart != null && filePart.exists()) {
                bodyBuilder.part("file", new FileSystemResource(filePart)).header(HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_OCTET_STREAM_VALUE);
                logger.debug("Added file part: {}", filePart.getName());
            }

            return webClient.post()
                    .uri(fullUrl)
                    .headers(headers -> addHeaders(headers, authToken, customHeaders)) // Use helper method
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .retrieve()
                    .toEntity(byte[].class)
                    .flatMap(response -> handleResponse(response, responseType, serializer))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)) // Basic retry logic (3 attempts, 1s delay)
                            .filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable) // Example:
                                                                                                                     // Retry
                                                                                                                     // only
                                                                                                                     // on
                                                                                                                     // 503
                            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                logger.error("Retry exhausted for request to {}: {}", fullUrl,
                                        retrySignal.failure().getMessage());
                                return retrySignal.failure(); // Throw the last error after retries are exhausted
                            }))
                    .block();

        } catch (Exception e) {
            // Log the root cause if available, especially for WebClient exceptions
            Throwable rootCause = e;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
            }
            logger.error("Error sending request to {}: {} (Root cause: {})", fullUrl, e.getMessage(),
                    rootCause.getMessage(), e);
            throw new RuntimeException("Failed to send request: " + rootCause.getMessage(), e);
        }
    }

    private Mono<MultipartResponseWrapper<R>> handleResponse(ResponseEntity<byte[]> response, Class<R> responseType,
            Serializer serializer) {
        if (!response.getStatusCode().is2xxSuccessful()) {
            String errorBody = new String(response.getBody() != null ? response.getBody() : new byte[0],
                    StandardCharsets.UTF_8);
            logger.error("Request failed with status: {} - Body: {}", response.getStatusCode(), errorBody);
            return Mono.error(new RuntimeException(
                    "Request failed with status: " + response.getStatusCode() + ", Body: " + errorBody));
        }

        MediaType contentType = response.getHeaders().getContentType();
        logger.debug("Response content type: {}", contentType);

        if (contentType != null && contentType.toString().startsWith("multipart/mixed")) {
            logger.debug("Handling multipart/mixed response");
            return handleMultipartMixedResponse(response, responseType, serializer);
        } else if (contentType != null && isBinaryContentType(contentType.toString())) {
            return handleBinaryResponse(response);
        } else {
            // Assume JSON or other text-based if not explicitly binary or multipart
            return handleJsonResponse(response, responseType, serializer);
        }
    }

    private Mono<MultipartResponseWrapper<R>> handleBinaryResponse(ResponseEntity<byte[]> response) {
        if (response.getBody() == null) {
            logger.warn("Empty binary response body received");
            return Mono.just(new MultipartResponseWrapper<>(null, null, null, null)); // No file, no identifier
        }
        try {
            // Extract the file content
            byte[] bodyBytes = response.getBody();
            String filename = extractFilename(response.getHeaders());
            String contentType = response.getHeaders().getContentType() != null
                    ? response.getHeaders().getContentType().toString()
                    : MediaType.APPLICATION_OCTET_STREAM_VALUE;

            // Wrap the file content in an InputStream
            InputStream fileContentStream = new ByteArrayInputStream(bodyBytes);

            // Store the file via StorageManager (if needed)
            ByteArrayMultipartFile multipartFile = new ByteArrayMultipartFile(bodyBytes, "file", filename, contentType);
            String storageIdentifier = storageManager.store(multipartFile, Collections.emptyMap(), "tmp-filesystem");
            logger.debug("Stored binary response via StorageManager. Identifier: {}", storageIdentifier);

            // Return both the metadata and the file content
            return Mono.just(new MultipartResponseWrapper<>(null, fileContentStream, storageIdentifier, filename));
        } catch (IOException e) {
            logger.error("Error storing binary response via StorageManager: {}", e.getMessage(), e);
            return Mono.error(new RuntimeException("Failed to store binary response", e));
        }
    }

    private Mono<MultipartResponseWrapper<R>> handleJsonResponse(ResponseEntity<byte[]> response, Class<R> responseType,
            Serializer serializer) {
        if (response.getBody() == null) {
            logger.warn("Empty JSON response body received");
            return Mono.just(new MultipartResponseWrapper<>(null, null, null, null));
        }
        String responseBody = new String(response.getBody(), StandardCharsets.UTF_8);
        try {
            R deserializedObject;
            if (Map.class.isAssignableFrom(responseType)) {
                Map<String, Object> resultMap = serializer.deserializeToMap(responseBody);
                if (responseType.isInstance(resultMap)) {
                    deserializedObject = responseType.cast(resultMap);
                } else {
                    logger.error("Type mismatch: Cannot cast deserialized Map to {}", responseType.getName());
                    return Mono.error(new RuntimeException("Type mismatch in JSON response handling"));
                }
            } else {
                deserializedObject = serializer.deserialize(responseBody, responseType);
            }
            return Mono.just(new MultipartResponseWrapper<>(deserializedObject, null, null, null)); // No file, no
                                                                                                    // identifier
        } catch (Exception e) {
            logger.error("Error deserializing JSON response: {}", e.getMessage(), e);
            return Mono.error(new RuntimeException("Failed to deserialize JSON response: " + e.getMessage(), e));
        }
    }

    private Mono<MultipartResponseWrapper<R>> handleMultipartMixedResponse(
            ResponseEntity<byte[]> response, Class<R> responseType, Serializer serializer) {

        MediaType contentType = response.getHeaders().getContentType();
        String boundary = contentType != null ? contentType.getParameter("boundary") : null;
        byte[] bodyBytes = response.getBody();

        if (boundary == null || boundary.isBlank() || bodyBytes == null) { // Added isBlank check
            logger.error("Invalid multipart/mixed response: Missing or empty boundary or body");
            return Mono.error(new RuntimeException("Invalid multipart/mixed response: Missing boundary or body"));
        }

        try {
            byte[] boundaryBytes = ("--" + boundary).getBytes(StandardCharsets.UTF_8);
            byte[] crlf = "\r\n".getBytes(StandardCharsets.UTF_8);
            byte[] doubleCrlf = "\r\n\r\n".getBytes(StandardCharsets.UTF_8);
            InputStream fileContentStream = null;
            R metadataPart = null;
            String storageIdentifier = null; // Changed from File
            String originalFilename = null;

            int currentPos = 0;
            // Find the first boundary
            int boundaryPos = findByteSequence(bodyBytes, boundaryBytes, currentPos);
            if (boundaryPos == -1) {
                logger.error("Invalid multipart format: Could not find initial boundary '{}'",
                        new String(boundaryBytes, StandardCharsets.UTF_8));
                return Mono.error(new RuntimeException("Invalid multipart format: No initial boundary"));
            }
            currentPos = boundaryPos + boundaryBytes.length;

            // Skip CRLF after first boundary
            if (isByteSequenceAt(bodyBytes, crlf, currentPos)) {
                currentPos += crlf.length;
            }

            while (currentPos < bodyBytes.length) {
                // Find end of headers (double CRLF)
                int headerEndPos = findByteSequence(bodyBytes, doubleCrlf, currentPos);
                if (headerEndPos == -1) {
                    logger.warn(
                            "Malformed multipart part: Could not find header end (double CRLF) starting at position {}. Stopping parse.",
                            currentPos);
                    break; // Malformed part
                }

                // Extract headers
                byte[] headerBytes = Arrays.copyOfRange(bodyBytes, currentPos, headerEndPos);
                String headersString = new String(headerBytes, StandardCharsets.UTF_8);
                String partContentType = null;
                String partDisposition = null;
                String[] headerLines = headersString.split("\r\n");
                for (String line : headerLines) {
                    if (line.toLowerCase().startsWith("content-type:")) {
                        partContentType = line.substring("content-type:".length()).trim();
                    } else if (line.toLowerCase().startsWith("content-disposition:")) {
                        partDisposition = line.substring("content-disposition:".length()).trim();
                    }
                }

                int bodyStartPos = headerEndPos + doubleCrlf.length;

                // Find the next boundary to determine the end of this part's body
                int nextBoundaryPos = findByteSequence(bodyBytes, boundaryBytes, bodyStartPos);
                int bodyEndPos;

                if (nextBoundaryPos == -1) {
                    byte[] finalBoundaryMarkerBytes = ("--" + boundary + "--").getBytes(StandardCharsets.UTF_8);
                    int finalMarkerPos = findByteSequence(bodyBytes, finalBoundaryMarkerBytes, bodyStartPos);

                    if (finalMarkerPos != -1) {
                        // Found the final marker. Body ends *before* the preceding CRLF, if it exists.
                        if (finalMarkerPos >= crlf.length
                                && isByteSequenceAt(bodyBytes, crlf, finalMarkerPos - crlf.length)) {
                            bodyEndPos = finalMarkerPos - crlf.length;
                        } else {
                            bodyEndPos = finalMarkerPos;
                            logger.warn("Final boundary marker '--{}--' found at {} without preceding CRLF.", boundary,
                                    finalMarkerPos);
                        }
                    } else {
                        // Could not find the final boundary marker, this indicates a malformed
                        // response.
                        logger.error(
                                "Could not find closing boundary marker ('--{}--'). Multipart message is likely truncated or malformed.",
                                boundary);
                        return Mono.error(
                                new RuntimeException("Invalid multipart format: Missing closing boundary marker"));
                    }
                    nextBoundaryPos = bodyBytes.length; // Ensure loop terminates after this part

                } else { // nextBoundaryPos was found initially (it's a regular boundary "--boundary")
                    // Found next boundary ("--boundary"). Body ends before the preceding CRLF.
                    if (nextBoundaryPos >= crlf.length
                            && isByteSequenceAt(bodyBytes, crlf, nextBoundaryPos - crlf.length)) {
                        bodyEndPos = nextBoundaryPos - crlf.length;
                    } else {
                        // This case should ideally not happen in a well-formed multipart message
                        logger.warn(
                                "Next boundary found at {} but not preceded by CRLF. Multipart format might be invalid.",
                                nextBoundaryPos);
                        bodyEndPos = nextBoundaryPos;
                    }
                    // Sanity check: ensure bodyEndPos is not before bodyStartPos
                    if (bodyEndPos < bodyStartPos) {
                        logger.error(
                                "Calculated body end position ({}) is before start position ({}). Invalid multipart format.",
                                bodyEndPos, bodyStartPos);
                        return Mono.error(new RuntimeException("Invalid multipart format: Body end before start"));
                    }
                }

                // Extract the actual body bytes for this part
                byte[] bodyPartBytes = Arrays.copyOfRange(bodyBytes, bodyStartPos, bodyEndPos);

                // Process based on headers
                if (partContentType != null && partContentType.startsWith("application/json")) {
                    try {
                        String jsonBody = new String(bodyPartBytes, StandardCharsets.UTF_8);
                        metadataPart = serializer.deserialize(jsonBody, responseType);
                        logger.debug("Deserialized JSON metadata part ({} bytes)", bodyPartBytes.length);
                    } catch (Exception e) {
                        logger.error("Failed to deserialize JSON part: {}", e.getMessage(), e);
                        return Mono.error(new RuntimeException("Failed to deserialize JSON part", e));
                    }
                } else if (partDisposition != null && partDisposition.contains("filename=")) {
                    // Check if a file part has already been processed and stored
                    if (storageIdentifier == null) { // Process only the first file part found
                        originalFilename = partDisposition.split("filename=")[1].replaceAll("\"", "").trim();
                        logger.debug("Found file part. Original Filename: '{}', Content-Type: '{}', Size: {} bytes",
                                originalFilename, partContentType, bodyPartBytes.length);
                        try {
                            // Use StorageManager to store the file part
                            String filePartContentType = partContentType != null ? partContentType
                                    : MediaType.APPLICATION_OCTET_STREAM_VALUE;
                            logger.trace("Creating ByteArrayMultipartFile wrapper.");
                            ByteArrayMultipartFile multipartFile = new ByteArrayMultipartFile(
                                    bodyPartBytes, "file", originalFilename, filePartContentType);
                            logger.trace("Calling storageManager.store...");
                            // Store using StorageManager (using temporary storage type)
                            storageIdentifier = storageManager.store(multipartFile, Collections.emptyMap(),
                                    "tmp-filesystem");
                            logger.debug("Successfully stored file part via StorageManager. Identifier: {}",
                                    storageIdentifier);

                        } catch (IOException e) {
                            logger.error("IOException storing file part ('{}') via StorageManager: {}",
                                    originalFilename, e.getMessage(), e);
                            return Mono.error(new RuntimeException("Failed to store file part due to IOException", e));
                        } catch (Exception e) { // Catch broader exceptions during storage
                            logger.error("Unexpected error storing file part ('{}') via StorageManager: {}",
                                    originalFilename, e.getMessage(), e);
                            return Mono.error(new RuntimeException("Unexpected error storing file part", e));
                        }
                    } else {
                        logger.warn("Multiple file parts found, ignoring subsequent file '{}'", partDisposition);
                    }
                    // Wrap the file content in an InputStream
                    fileContentStream = new ByteArrayInputStream(bodyPartBytes);
                } else {
                    logger.debug("Skipping multipart part with Content-Type: {}", partContentType);
                }

                // Move position to the start of the next part (after the boundary)
                currentPos = nextBoundaryPos + boundaryBytes.length;
                // Skip potential CRLF
                if (isByteSequenceAt(bodyBytes, crlf, currentPos)) {
                    currentPos += crlf.length;
                }
                // Check if we landed on the final boundary marker "--"
                if (isByteSequenceAt(bodyBytes, "--".getBytes(StandardCharsets.UTF_8), currentPos)) {
                    break; // End of multipart message
                }

            } // End while loop

            if (metadataPart == null && storageIdentifier == null) { // Check storageIdentifier
                logger.warn("Multipart response processed, but no JSON metadata or file part identified and stored.");
            }

            return Mono.just(
                    new MultipartResponseWrapper<>(metadataPart, fileContentStream, storageIdentifier, originalFilename));

        } catch (Exception e) {
            logger.error("Error parsing multipart/mixed response: {}", e.getMessage(), e);
            return Mono.error(new RuntimeException("Failed to parse multipart/mixed response", e));
        }
    }

    // Helper function to find byte sequence start index from a given position
    private int findByteSequence(byte[] haystack, byte[] needle, int startIndex) {
        if (needle.length == 0 || startIndex < 0 || startIndex > haystack.length - needle.length)
            return -1;
        for (int i = startIndex; i <= haystack.length - needle.length; i++) {
            boolean found = true;
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    found = false;
                    break;
                }
            }
            if (found)
                return i;
        }
        return -1;
    }

    // Helper to check if a byte sequence exists at a specific position
    private boolean isByteSequenceAt(byte[] haystack, byte[] needle, int startIndex) {
        if (startIndex < 0 || needle.length == 0 || startIndex + needle.length > haystack.length) {
            return false;
        }
        for (int j = 0; j < needle.length; j++) {
            if (haystack[startIndex + j] != needle[j]) {
                return false;
            }
        }
        return true;
    }

    // --- Restored Helper Methods ---

    @Override
    public void sendAsync(T jsonPart, File filePart) {
        sendAsyncWithHeaders(jsonPart, filePart, Collections.emptyMap());
    }

    @Override
    public void sendAsync(T jsonPart, File filePart, Map<String, String> headers) {
        sendAsyncWithHeaders(jsonPart, filePart, headers);
    }

    private void sendAsyncWithHeaders(T jsonPart, File filePart, Map<String, String> headers) {
        CompletableFuture.runAsync(() -> {
            try {
                String fullUrl = buildFullUrl(headers);
                MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
                Serializer serializer = getSerializer(headers);

                if (jsonPart != null) {
                    String json = serializer.serialize(jsonPart);
                    bodyBuilder.part("metadata", json).header(HttpHeaders.CONTENT_TYPE, serializer.getContentType());
                }

                if (filePart != null && filePart.exists()) {
                    bodyBuilder.part("file", new FileSystemResource(filePart)).header(HttpHeaders.CONTENT_TYPE,
                            MediaType.APPLICATION_OCTET_STREAM_VALUE);
                }

                webClient.post()
                        .uri(fullUrl)
                        .headers(h -> addHeaders(h, null, headers)) // Auth token not typically sent with
                                                                    // fire-and-forget async
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                        .retrieve()
                        .bodyToMono(byte[].class) // Consume the body to execute the request
                        .subscribe(
                                responseBody -> logger.debug(
                                        "Async request completed successfully (response body size: {})",
                                        responseBody != null ? responseBody.length : 0),
                                error -> logger.error("Async request failed: {}", error.getMessage(), error) // Log full
                                                                                                             // error
                );
            } catch (Exception e) {
                logger.error("Error initiating async request: {}", e.getMessage(), e);
            }
        });
    }

    private String buildFullUrl(Map<String, String> customHeaders) {
        // Create a mutable copy if needed, or ensure the original map is not modified
        // if passed elsewhere
        Map<String, String> mutableHeaders = (customHeaders != null) ? new HashMap<>(customHeaders) : new HashMap<>();
        String pathSuffix = mutableHeaders.remove("X-Path"); // Remove X-Path if present

        if (pathSuffix != null) {
            // Ensure no double slashes
            String baseUrl = this.endpoint.endsWith("/") ? this.endpoint.substring(0, this.endpoint.length() - 1)
                    : this.endpoint;
            String cleanPathSuffix = pathSuffix.startsWith("/") ? pathSuffix.substring(1) : pathSuffix;
            return baseUrl + "/" + cleanPathSuffix;
        }
        return this.endpoint;
    }

    private void addHeaders(HttpHeaders headers, String authToken, Map<String, String> customHeaders) {
        if (authToken != null && !authToken.isBlank()) {
            // Ensure Bearer prefix is present if needed by the receiving API
            String authHeaderValue = authToken.toLowerCase().startsWith("bearer ") ? authToken : "Bearer " + authToken;
            headers.setBearerAuth(authHeaderValue.substring("Bearer ".length())); // setBearerAuth adds the prefix
        }
        // Add other custom headers, excluding X-Path as it's used for URL building
        if (customHeaders != null) {
            customHeaders.forEach((key, value) -> {
                if (!"X-Path".equalsIgnoreCase(key)) { // Case-insensitive check
                    headers.add(key, value);
                }
            });
        }
    }

    private Serializer getSerializer(Map<String, String> customHeaders) {
        String contentType = (customHeaders != null) ? customHeaders.get("Content-Type") : null;
        // Default to JSON serializer if no content type specified in headers or if it's
        // empty
        String effectiveContentType = (contentType != null && !contentType.isBlank()) ? contentType
                : MediaType.APPLICATION_JSON_VALUE;
        return serializerFactory.getSerializerByContentType(effectiveContentType);
    }
}
