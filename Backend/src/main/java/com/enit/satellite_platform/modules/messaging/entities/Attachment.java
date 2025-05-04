package com.enit.satellite_platform.modules.messaging.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Represents metadata for a file attached to a message.
 * The actual file content is stored separately (e.g., local filesystem or cloud storage).
 */
@Data // Generates getters, setters, toString, equals, hashCode
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "message_attachments") // Optional: specify collection name if different from class name
public class Attachment {

    @Id
    private String id;

    private String messageId; // Link back to the message it belongs to

    private String filename; // Original filename

    private String fileType; // MIME type (e.g., "image/jpeg", "application/pdf")

    private long fileSize; // Size in bytes

    private String storagePath; // Path to the file in the local storage system (e.g., "messaging/user123/abc.jpg")
                                // Could be replaced with storageUrl if using cloud storage like S3.

    // Consider adding upload timestamp, uploader ID if needed
}
