package com.enit.satellite_platform.modules.messaging.controller;

import com.enit.satellite_platform.modules.messaging.service.AttachmentService;
import com.enit.satellite_platform.modules.messaging.service.MessagingService;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.User;
import com.enit.satellite_platform.modules.user_management.normal_user_service.repositories.UserRepository;
import com.enit.satellite_platform.modules.messaging.dto.SendMessageRequest;
import com.enit.satellite_platform.modules.messaging.entities.Attachment;
import com.enit.satellite_platform.modules.messaging.entities.Conversation;
import com.enit.satellite_platform.modules.messaging.entities.Message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/messaging")
@RequiredArgsConstructor
@Slf4j
public class MessagingController {

    private final MessagingService messagingService;
    private final AttachmentService attachmentService;
    private final UserRepository userRepository;

    /**
     * Sends a new message.
     * The sender is determined from the authenticated user context.
     */
    @PostMapping("/messages")
    @PreAuthorize("hasAnyRole('THEMATICIAN', 'ADMIN')") // Allow both users and admins to send messages
    public ResponseEntity<?> sendMessage(@RequestBody SendMessageRequest request) { // Return ResponseEntity<?> for
                                                                                    // better error handling
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String senderEmail = authentication.getName(); // This is the email

        // Find the actual user by email to get their MongoDB ObjectId
        Optional<User> senderOpt = userRepository.findByEmail(senderEmail);
        if (senderOpt.isEmpty()) {
            log.error("Authenticated user with email {} not found in database.", senderEmail);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authenticated user not found."));
        }
        String senderId = senderOpt.get().getId().toString(); // Get the actual ObjectId string

        log.info("Received request to send message from user ID {} (email: {}) to {} of type {}", senderId, senderEmail,
                request.getRecipientId(), request.getMessageType());

        // Basic validation
        if (request.getContent() == null || request.getContent().isBlank()) {
            return ResponseEntity.badRequest().build(); // Or return error response
        }
        if (request.getRecipientId() == null || request.getRecipientId().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (request.getMessageType() == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Message sentMessage = messagingService.sendMessage(
                    senderId,
                    request.getRecipientId(),
                    request.getContent(),
                    request.getMessageType());
            // Return the message object (as sent to queue, not guaranteed saved yet)
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(sentMessage);
        } catch (IllegalArgumentException | IllegalStateException e) { // Catch IllegalStateException too
            log.error("Error sending message: {}", e.getMessage());
            // Return specific error messages
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Internal server error sending message", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "An unexpected error occurred."));
        }
    }

    /**
     * Retrieves all conversations for the currently authenticated user.
     */
    @GetMapping("/conversations")
    @PreAuthorize("hasAnyRole('THEMATICIAN', 'ADMIN')")
    public ResponseEntity<?> getUserConversations() { // Return ResponseEntity<?>
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        Optional<User> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            log.error("Authenticated user with email {} not found in database.", userEmail);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authenticated user not found."));
        }
        String userId = userOpt.get().getId().toString();

        log.info("Fetching conversations for user ID {} (email: {})", userId, userEmail);
        try {
            List<Conversation> conversations = messagingService.getConversationsForUser(userId); // Pass the actual ID
            return ResponseEntity.ok(conversations);
        } catch (IllegalArgumentException e) {
            log.error("Error fetching conversations for user ID {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); // Return specific error
        }
    }

    /**
     * Retrieves a specific conversation by ID.
     * Ensures the authenticated user is a participant.
     */
    @GetMapping("/conversations/{conversationId}")
    @PreAuthorize("hasAnyRole('THEMATICIAN', 'ADMIN')")
    public ResponseEntity<?> getConversation(@PathVariable String conversationId) { // Return ResponseEntity<?>
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        Optional<User> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            log.error("Authenticated user with email {} not found in database.", userEmail);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authenticated user not found."));
        }
        String userId = userOpt.get().getId().toString(); // Get the actual ID

        log.info("Fetching conversation {} for user ID {} (email: {})", conversationId, userId, userEmail);
        Optional<Conversation> conversationOpt = messagingService.getConversationById(conversationId);

        if (conversationOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Conversation conversation = conversationOpt.get();
        // Security check: Ensure the current user is part of the conversation
        if (!conversation.getParticipants().contains(userId)) {
            log.warn("User {} attempted to access conversation {} they are not part of.", userId, conversationId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(conversation);
    }

    /**
     * Uploads an attachment for a specific message.
     */
    @PostMapping("/conversations/{conversationId}/messages/{messageId}/attachments")
    @PreAuthorize("hasAnyRole('THEMATICIAN', 'ADMIN')")
    public ResponseEntity<?> uploadAttachment(@PathVariable String conversationId,
            @PathVariable String messageId,
            @RequestParam("file") MultipartFile file) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        Optional<User> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            log.error("Authenticated user with email {} not found in database.", userEmail);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authenticated user not found."));
        }
        String userId = userOpt.get().getId().toString(); // Get the actual ID

        log.info("User ID {} (email: {}) uploading attachment for message {} in conversation {}", userId, userEmail,
                messageId, conversationId);

        // Security check: Ensure user is part of the conversation (optional, depends on
        // requirements)
        Optional<Conversation> conversationOpt = messagingService.getConversationById(conversationId);
        if (conversationOpt.isEmpty() || !conversationOpt.get().getParticipants().contains(userId)) {
            log.warn("User {} attempted upload to conversation {} they are not part of.", userId, conversationId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Attachment attachment = attachmentService.storeAttachment(file, conversationId, messageId, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(attachment);
        } catch (IllegalArgumentException e) {
            log.error("Bad request during attachment upload: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            log.error("Failed to store attachment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload attachment.");
        }
    }

    /**
     * Downloads a specific attachment.
     */
    @GetMapping("/conversations/{conversationId}/messages/{messageId}/attachments/{attachmentId}")
    @PreAuthorize("hasAnyRole('THEMATICIAN', 'ADMIN')")
    public ResponseEntity<?> downloadAttachment(@PathVariable String conversationId, // Change return type to
                                                                                     // ResponseEntity<?>
            @PathVariable String messageId,
            @PathVariable String attachmentId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        Optional<User> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            log.error("Authenticated user with email {} not found in database.", userEmail);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authenticated user not found.")); // This is now compatible
        }
        String userId = userOpt.get().getId().toString(); // Get the actual ID

        log.info("User ID {} (email: {}) requesting download of attachment {} from message {} in conversation {}",
                userId, userEmail, attachmentId, messageId, conversationId);

        // Security check: Ensure user is part of the conversation
        Optional<Conversation> conversationOpt = messagingService.getConversationById(conversationId);
        if (conversationOpt.isEmpty() || !conversationOpt.get().getParticipants().contains(userId)) {
            log.warn("User {} attempted download from conversation {} they are not part of.", userId, conversationId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<Path> attachmentPathOpt = attachmentService.getAttachmentPath(conversationId, messageId, attachmentId);

        if (attachmentPathOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path filePath = attachmentPathOpt.get();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                // Try to determine file's content type
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream"; // Default content type
                }

                // Find original filename from metadata
                String originalFilename = conversationOpt.get().getMessages().stream()
                        .filter(m -> m.getId().equals(messageId)).findFirst()
                        .flatMap(m -> m.getAttachments().stream().filter(a -> a.getId().equals(attachmentId))
                                .findFirst())
                        .map(Attachment::getFilename)
                        .orElse(filePath.getFileName().toString()); // Fallback to stored filename

                return ResponseEntity.ok()
                        .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalFilename + "\"")
                        .body(resource);
            } else {
                log.error("Could not read attachment file: {}", filePath);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            log.error("Error downloading attachment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // TODO: Add endpoints for adding/removing reactions

}
