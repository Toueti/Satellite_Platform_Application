package com.enit.satellite_platform.modules.messaging.controller;

import com.enit.satellite_platform.modules.messaging.model.Attachment; // Added import
import com.enit.satellite_platform.modules.messaging.model.Conversation;
import com.enit.satellite_platform.modules.messaging.model.Message;
import com.enit.satellite_platform.modules.messaging.model.MessageType; // Assuming MessageType is needed for request
import com.enit.satellite_platform.modules.messaging.service.AttachmentService;
import com.enit.satellite_platform.modules.messaging.service.MessagingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication; // To get current user
import org.springframework.security.core.context.SecurityContextHolder; // To get current user
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; // For attachments

import java.io.IOException;
import java.nio.file.Files; // Added import
import java.nio.file.Path; // For attachment download
import org.springframework.core.io.Resource; // For attachment download
import org.springframework.core.io.UrlResource; // For attachment download
import org.springframework.http.HttpHeaders; // For attachment download


import java.util.List;
import java.util.Optional;

// Define a DTO for sending messages
@lombok.Data
class SendMessageRequest {
    private String recipientId;
    private String content;
    private MessageType messageType; // e.g., USER_TO_USER, USER_TO_ADMIN
}

// Define a DTO for adding reactions (example)
@lombok.Data
class AddReactionRequest {
    private String reactionType; // e.g., "LIKE"
}


@RestController
@RequestMapping("/api/v1/messaging") // Base path for messaging endpoints
@RequiredArgsConstructor
@Slf4j
public class MessagingController {

    private final MessagingService messagingService;
    private final AttachmentService attachmentService;

    /**
     * Sends a new message.
     * The sender is determined from the authenticated user context.
     */
    @PostMapping("/messages")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')") // Allow both users and admins to send messages
    public ResponseEntity<Message> sendMessage(@RequestBody SendMessageRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String senderId = authentication.getName(); // Assuming username/ID is stored in 'name'

        log.info("Received request to send message from {} to {} of type {}", senderId, request.getRecipientId(), request.getMessageType());

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
                    request.getMessageType()
            );
            // Return the message object (as sent to queue, not guaranteed saved yet)
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(sentMessage);
        } catch (IllegalArgumentException e) {
            log.error("Error sending message: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null); // Consider a proper error response DTO
        } catch (Exception e) {
            log.error("Internal server error sending message", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Retrieves all conversations for the currently authenticated user.
     */
    @GetMapping("/conversations")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<Conversation>> getUserConversations() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        log.info("Fetching conversations for user {}", userId);
        try {
            List<Conversation> conversations = messagingService.getConversationsForUser(userId);
            return ResponseEntity.ok(conversations);
        } catch (IllegalArgumentException e) {
             log.error("Error fetching conversations for user {}: {}", userId, e.getMessage());
            return ResponseEntity.notFound().build(); // If user validation fails
        }
    }

    /**
     * Retrieves a specific conversation by ID.
     * Ensures the authenticated user is a participant.
     */
    @GetMapping("/conversations/{conversationId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Conversation> getConversation(@PathVariable String conversationId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("Fetching conversation {} for user {}", conversationId, userId);
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
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> uploadAttachment(@PathVariable String conversationId,
                                              @PathVariable String messageId,
                                              @RequestParam("file") MultipartFile file) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("User {} uploading attachment for message {} in conversation {}", userId, messageId, conversationId);

        // Security check: Ensure user is part of the conversation (optional, depends on requirements)
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
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable String conversationId,
                                                      @PathVariable String messageId,
                                                      @PathVariable String attachmentId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("User {} requesting download of attachment {} from message {} in conversation {}", userId, attachmentId, messageId, conversationId);

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
                    .flatMap(m -> m.getAttachments().stream().filter(a -> a.getId().equals(attachmentId)).findFirst())
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
