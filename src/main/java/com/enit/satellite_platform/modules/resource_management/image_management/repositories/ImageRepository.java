package com.enit.satellite_platform.modules.resource_management.image_management.repositories;

import com.enit.satellite_platform.modules.project_management.model.Project;
import com.enit.satellite_platform.modules.resource_management.image_management.models.Image;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Image entities in MongoDB.
 */
@Repository
public interface ImageRepository extends MongoRepository<Image, String> {

    /**
     * Check if an image exists by its ID and project ID.
     */
    boolean existsByImageIdAndProject_ProjectId(String imageId, ObjectId projectId);

    /**
     * Find an image by its name and project ID.
     */
    Optional<Image> findByImageNameAndProject_ProjectId(String imageName, ObjectId projectId);

    /**
     * Find all images associated with a project ID.
     */
    @Query("{ 'project.$id': ?0 }")
    List<Image> findAllByProject_ProjectId(ObjectId projectId);

    /**
     * Find all images associated with a project ID with pagination.
     */
    @Query("{ 'project.$id': ?0 }")
    Page<Image> findAllByProject_ProjectId(ObjectId projectId, Pageable pageable);

    /**
     * Delete all images associated with a project ID.
     */
    void deleteAllByProject_ProjectId(ObjectId projectId);

    /**
     * Find an image by its ID and project ID.
     */
    Optional<Image> findByImageIdAndProject_ProjectId(String imageId, ObjectId projectId);

    /**
     * Delete an image by its ID and project ID.
     */
    void deleteByImageIdAndProject_ProjectId(String imageId, ObjectId projectId);

    /**
     * Find an image by its name (across all projects).
     */
    Optional<Image> findByImageName(String imageName);

    /**
     * Count images associated with a project entity.
     */
    long countByProject(Project project);

    /**
     * Count images associated with a project ID.
     */
    @Query(value = "{ 'project.$id': ?0 }", count = true)
    long countByProject_ProjectId(ObjectId projectId);

    /**
     * Check if an image exists by its ID.
     */
    boolean existsByImageId(String imageId);

    /**
     * Delete multiple images by their IDs.
     */
    void deleteAllByImageIdIn(List<String> imageIds);

    /**
     * Find an image by its name and project ID.
     *
     * @param imageName The name of the image.
     * @param projectId The ID of the project.
     * @return An optional containing the image if found, empty otherwise.
     */
    @Query("{ 'project.$projectId': ?0, 'imageName': ?1 }")
    Optional<Image> findByNameAndProjectId(String imageName, ObjectId projectId);

    /**
     * Checks if an image exists by its name and project ID.
     *
     * @param imageName The name of the image.
     * @param projectId The ID of the project.
     * @return True if the image exists, false otherwise.
     */

    @Query("{ 'project.$projectId': ?0, 'imageName': ?1 }")
    boolean existsByNameAndProjectId(String imageName, ObjectId projectId);

    /**
     * Find all images where the associated project's owner ID matches the given user ID.
     * Note: Assumes owner ID in Project is stored as ObjectId.
     *
     * @param ownerId The ObjectId of the owner user, as a String.
     * @return A list of images owned by the user.
     */
    @Query("{ 'project.owner.$id' : ?0 }") // Query based on the referenced project's owner's ID
    List<Image> findAllByOwnerId(String ownerId);
}
