package com.enit.satellite_platform.modules.resource_management.image_management.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.enit.satellite_platform.modules.project_management.entities.Project;
import com.enit.satellite_platform.shared.model.SoftDeletable; // Import SoftDeletable
import com.fasterxml.jackson.annotation.JsonManagedReference; // Import Jackson annotation

import java.util.Date;
import java.util.List;
import java.util.Map;

@Document(collection = "images")
@Data
public class Image implements SoftDeletable { // Implement SoftDeletable
    @Id
    private String imageId;

    @Indexed
    @Field("imageName")
    private String imageName;

    @Field("storageIdentifier") // Generic identifier (could be GridFS ID, file path, S3 URI, etc.)
    private String storageIdentifier;

    @Field("storageType") // Indicates where the file is stored (e.g., "gridfs", "filesystem", "s3")
    private String storageType;

    @Field("fileSize")
    private long fileSize; // Added field to store image size in bytes

    @Field("requestTime")
    private Date requestTime;

    @Field("updatedAt")
    private Date updatedAt;

    @Field("metadata")
    private Map<String, Object> metadata;

    @DBRef(lazy = true)
    @Field("results")
    @JsonManagedReference // Add annotation to manage the 'parent' side of the relationship
    private List<ProcessingResults> results;

    @DBRef
    @Field("project")
    private Project project;

    @Field("deleted")
    private boolean deleted = false; // Soft delete flag

    @Field("deletedAt")
    private Date deletedAt; // Timestamp for soft deletion

    protected void onCreate() {
        requestTime = new Date();
    }

    protected void onUpdate() {
        updatedAt = new Date();
    }

    // Implement SoftDeletable interface method
    @Override
    public Object getId() {
        return this.imageId;
    }
}
