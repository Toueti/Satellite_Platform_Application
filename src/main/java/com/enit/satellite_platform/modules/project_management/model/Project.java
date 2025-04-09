package com.enit.satellite_platform.modules.project_management.model;

import com.enit.satellite_platform.modules.resource_management.image_management.models.Image;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.models.User;
import com.enit.satellite_platform.shared.converters.UserKeyDeserializer; // Import the custom deserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize; // Import annotation

import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents a project in the satellite platform.
 * A project is owned by a user and can be shared with other users.
 * It contains information such as name, description, creation and update
 * timestamps,
 * and a set of associated images.
 */
@CompoundIndexes({
    @CompoundIndex(name = "owner_projectName_unique", 
                   def = "{'owner': 1, 'projectName': 1}", 
                   unique = true)
})
@Document(collection = "projects")
@Getter
@Setter
public class Project {

    /**
     * The unique identifier of the project.
     */
    @Id
    private ObjectId id;

    /**
     * The user who owns the project.
     */
    @DBRef
    @Field("owner")
    private User owner;

    /**
     * The name of the project.
     */
    @Field("projectName")
    private String projectName;

    /**
     * A description of the project.
     */
    @Field("description")
    private String description;

    /**
     * The date and time when the project was created.
     */
    @Field("createdAt")
    @CreatedDate
    private Date createdAt;

    /**
     * The date and time when the project was last updated.
     */
    @Field("updatedAt")
    @LastModifiedDate
    private Date updatedAt;

    /**
     * The date and time when the project was last accessed.
     */
    @Field("lastAccessedTime")
    private Date lastAccessedTime;

    /**
     * Indicates whether the project is archived.
     */
    @Field("archived")
    private boolean archived = false; // New field for archiving

    /**
     * The date and time when the project was archived.
     */
    @Field("archivedDate")
    private Date archivedDate; // New field for archiving

    /**
     * A set of tags associated with the project.
     */
    @Field("tags")
    private Set<String> tags = new HashSet<>(); // New field for tagging

    /**
     * The status of the project.
     */
    @Field("status")
    private String status; 

    @Field("projectDirectory")
    private String projectDirectory;

    @Field("metadata")
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * A set of images associated with the project.
     * Uses lazy loading to improve performance.
     */
    @DBRef(lazy = true)
    private Set<Image> images = new HashSet<>();

    /**
     * A set of users with whom the project is shared.
     * Uses lazy loading to improve performance.
     */
    @DBRef(lazy = true)
    @JsonDeserialize(keyUsing = UserKeyDeserializer.class) // Use the custom deserializer for map keys
    private Map<User, PermissionLevel> sharedUsers = new HashMap<>();

    /**
     * Updates the last accessed time of the project to the current time.
     */

    @Override
    public String toString() {
        return "Project{" +
                "id=" + id +
                ", owner=" + owner +
                ", projectName='" + projectName + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", lastAccessedTime=" + lastAccessedTime +
                ", archived=" + archived +
                ", archivedDate=" + archivedDate +
                ", tags=" + tags +
                ", status='" + status + '\'' +
                ", projectDirectory='" + projectDirectory + '\'' +
                ", metadata=" + metadata +
                '}';
    }
    public String getId() {
        return id != null ? id.toHexString() : null;
    }
    public void updateLastAccessedTime() {
        this.lastAccessedTime = new Date();
    }

    /**
     * Adds an image to the project.
     *
     * @param image The image to add.
     */
    public void addImage(Image image) {
        this.images.add(image);
    }

   /**
     * Shares the project with the specified user and assigns a permission level.
     *
     * @param user     The user to share the project with.
     * @param permission The permission level to grant (e.g., "VIEWER", "EDITOR").
     */
    public void shareWith(User user, PermissionLevel permission) {
        if (!user.equals(this.owner)) {
            this.sharedUsers.put(user, permission);
        }
    }

    /**
     * Unshares the project with the specified user.
     *
     * @param user The user to unshare the project with.
     */
    public void unshareWith(User user) {
        this.sharedUsers.remove(user);
    }

    /**
     * Checks if the given user has access to the project.
     * Considers both ownership and explicit sharing.
     *
     * @param user The user to check.
     * @return True if the user has access, false otherwise.
     */
    public boolean hasAccess(User user) {
        return this.owner.equals(user) || this.sharedUsers.containsKey(user);
    }

    /**
     * Checks if the given user has at least READ access to the project.
     * Owners always have access. Shared users need READ or WRITE permission.
     *
     * @param user The user to check.
     * @return True if the user has read access, false otherwise.
     */
    public boolean hasAccess(User user, PermissionLevel requiredLevel) {
        if (this.owner.equals(user)) {
            return true; // Owner has all permissions
        }
        PermissionLevel grantedLevel = this.sharedUsers.get(user);
        if (grantedLevel == null) {
            return false; // Not shared with this user
        }
        // Check if granted level is sufficient
        return grantedLevel.includes(requiredLevel);
    }


    public String getProjectDirectory() {
        return projectDirectory;
    }

    public void setProjectDirectory(String projectDirectory) {
        this.projectDirectory = projectDirectory;
    }
}
