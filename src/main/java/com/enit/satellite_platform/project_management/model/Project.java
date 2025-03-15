package com.enit.satellite_platform.project_management.model;

import com.enit.satellite_platform.resources_management.models.Image;
import com.enit.satellite_platform.user_management.model.User;

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
import java.util.HashSet;
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
    @Field("projectID")
    private ObjectId projectID;

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
    private String status; // New field for status tracking

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
    private Set<User> sharedUsers = new HashSet<>();

    /**
     * Updates the last accessed time of the project to the current time.
     */
    public void updateLastAccessedTime() {
        lastAccessedTime = new Date();
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
     * Shares the project with the specified user.
     *
     * @param user The user to share the project with.
     */
    public void shareWith(User user) {
        if (!user.equals(owner)) {
            sharedUsers.add(user);
        }
    }

    /**
     * Unshares the project with the specified user.
     *
     * @param user The user to unshare the project with.
     */
    public void unshareWith(User user) {
        sharedUsers.remove(user);
    }

    /**
     * Checks if the given user has access to the project.
     *
     * @param user The user to check.
     * @return True if the user has access, false otherwise.
     */
    public boolean hasAccess(User user) {
        return owner.equals(user) || sharedUsers.contains(user);
    }
}
