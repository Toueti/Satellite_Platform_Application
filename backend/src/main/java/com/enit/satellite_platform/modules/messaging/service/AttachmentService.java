package com.enit.satellite_platform.modules.messaging.service;

import com.enit.satellite_platform.modules.messaging.entities.Attachment;
import com.enit.satellite_platform.modules.messaging.entities.Conversation;
import com.enit.satellite_platform.modules.messaging.entities.Message;
import com.enit.satellite_platform.modules.messaging.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope; // Import RefreshScope
import org.springframework.core.env.Environment; // Import Environment
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays; // For parsing allowed types
import java.util.HashSet; // For storing allowed types
import java.util.Optional;
import java.util.Set; // For storing allowed types
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@RefreshScope // Add RefreshScope annotation
public class AttachmentService {

    // Inject the base upload directory path from application properties
    // Example: file.upload-dir=upload-dir
    // Inject Environment to access properties dynamically
    private final Environment environment;
    private final ConversationRepository conversationRepository;

    // Keep baseUploadPath injection
    @Value("${file.upload-dir:upload-dir}")
    private String baseUploadPath;


    // Define property keys
    private static final String MAX_SIZE_MB_PROPERTY = "messaging.attachments.max.size";
    private static final String ALLOWED_TYPES_PROPERTY = "messaging.attachments.allowed.types";
    private static final long DEFAULT_MAX_SIZE_MB = 10; // Default 10 MB
    private static final String DEFAULT_ALLOWED_TYPES = "jpg,jpeg,png,pdf,doc,docx,txt"; // Default allowed types

    /**
     * Stores an uploaded file as an attachment for a specific message within a conversation.
     *
     * @param file           The uploaded file.
     * @param conversationId The ID of the conversation the message belongs to.
     * @param messageId      The ID of the message to attach the file to.
     * @param userId         The ID of the user uploading the file (for directory structure).
     * @return The created Attachment metadata object.
     * @throws IOException If file saving fails.
     * @throws IllegalArgumentException If conversation or message is not found.
     */
    public Attachment storeAttachment(MultipartFile file, String conversationId, String messageId, String userId) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file.");
        }

        // *** Attachment Validation ***
        validateAttachment(file);
        // **************************


        // 1. Find the conversation
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));

        // 2. Find the message within the conversation
        Message message = conversation.getMessages().stream()
                .filter(m -> m.getId().equals(messageId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId + " in conversation: " + conversationId));

        // 3. Prepare storage path
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = StringUtils.getFilenameExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + (fileExtension != null ? "." + fileExtension : "");
        // Store under baseUploadPath/messaging/{userId}/{messageId}/{uniqueFilename}
        Path userMessagePath = Paths.get(baseUploadPath, "messaging", userId, messageId);
        Files.createDirectories(userMessagePath); // Ensure directory exists
        Path destinationPath = userMessagePath.resolve(uniqueFilename);

        // 4. Save the file
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Saved attachment file to: {}", destinationPath);
        } catch (IOException e) {
            log.error("Failed to save attachment file {} for message {}: {}", originalFilename, messageId, e.getMessage(), e);
            throw new IOException("Could not save file: " + originalFilename, e);
        }

        // 5. Create Attachment metadata
        Attachment attachment = new Attachment();
        attachment.setId(UUID.randomUUID().toString());
        attachment.setMessageId(messageId);
        attachment.setFilename(originalFilename);
        attachment.setFileType(file.getContentType());
        attachment.setFileSize(file.getSize());
        // Store relative path from baseUploadPath
        attachment.setStoragePath(Paths.get("messaging", userId, messageId, uniqueFilename).toString().replace("\\", "/")); // Normalize path separators

        // 6. Add attachment metadata to the message and save conversation
        message.getAttachments().add(attachment);
        conversationRepository.save(conversation); // Save updated conversation
        log.info("Attachment metadata added to message {} and conversation {} saved.", messageId, conversationId);

        return attachment;
    }

    /**
     * Retrieves the file path for a given attachment.
     * (Actual file serving would likely happen in a controller).
     *
     * @param attachmentId The ID of the attachment.
     * @param conversationId The ID of the conversation.
     * @param messageId The ID of the message.
     * @return Optional containing the full Path to the file if found.
     */
    public Optional<Path> getAttachmentPath(String conversationId, String messageId, String attachmentId) {
         return conversationRepository.findById(conversationId)
                .flatMap(conversation -> conversation.getMessages().stream()
                        .filter(m -> m.getId().equals(messageId))
                        .findFirst()
                )
                .flatMap(message -> message.getAttachments().stream()
                        .filter(a -> a.getId().equals(attachmentId))
                        .findFirst()
                )
                .map(attachment -> Paths.get(baseUploadPath).resolve(attachment.getStoragePath()).normalize());
    }

    // TODO: Add method for deleting attachments (both file and metadata)


    // --- Helper Methods ---

    private void validateAttachment(MultipartFile file) {
        // Get configured limits with defaults
        long maxSizeBytes = getMaxAttachmentSizeBytes();
        Set<String> allowedTypes = getAllowedAttachmentTypes();

        // Validate size
        if (file.getSize() > maxSizeBytes) {
            log.warn("Attachment upload failed: File size {} exceeds limit {} bytes.", file.getSize(), maxSizeBytes);
            throw new IllegalArgumentException("File exceeds maximum allowed size of " + (maxSizeBytes / 1024 / 1024) + " MB.");
        }

        // Validate type (check both MIME type and file extension)
        String contentType = file.getContentType();
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = StringUtils.getFilenameExtension(filename);

        boolean typeAllowed = false;
        if (contentType != null && allowedTypes.contains(contentType.toLowerCase())) {
            typeAllowed = true;
        }
        if (!typeAllowed && extension != null && allowedTypes.contains(extension.toLowerCase())) {
            typeAllowed = true;
        }

        if (!typeAllowed) {
             log.warn("Attachment upload failed: File type '{}' (extension '{}') is not allowed. Allowed types: {}", contentType, extension, allowedTypes);
            throw new IllegalArgumentException("File type not allowed. Allowed types are: " + String.join(", ", allowedTypes));
        }

        log.debug("Attachment validation passed for file: {}", filename);
    }

    private long getMaxAttachmentSizeBytes() {
        String maxSizeMbStr = environment.getProperty(MAX_SIZE_MB_PROPERTY, String.valueOf(DEFAULT_MAX_SIZE_MB));
        try {
            long maxSizeMb = Long.parseLong(maxSizeMbStr);
            return maxSizeMb * 1024 * 1024; // Convert MB to bytes
        } catch (NumberFormatException e) {
            log.error("Invalid format for property '{}': '{}'. Using default value {} MB.", MAX_SIZE_MB_PROPERTY, maxSizeMbStr, DEFAULT_MAX_SIZE_MB, e);
            return DEFAULT_MAX_SIZE_MB * 1024 * 1024;
        }
    }

    private Set<String> getAllowedAttachmentTypes() {
        String allowedTypesStr = environment.getProperty(ALLOWED_TYPES_PROPERTY, DEFAULT_ALLOWED_TYPES);
        if (allowedTypesStr == null || allowedTypesStr.isBlank()) {
            return new HashSet<>(Arrays.asList(DEFAULT_ALLOWED_TYPES.split(","))); // Use default if property is empty
        }
        // Split by comma, trim whitespace, convert to lowercase, and collect into a Set
        return new HashSet<>(Arrays.asList(allowedTypesStr.toLowerCase().split("\\s*,\\s*")));
    }
}
