package com.enit.satellite_platform.resources_management.repositories;

import com.enit.satellite_platform.project_management.model.Project;
import com.enit.satellite_platform.resources_management.models.Image;
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
    boolean existsByImageIdAndProject_ProjectID(String imageId, ObjectId projectId);

    /**
     * Find an image by its name and project ID.
     */
    Optional<Image> findByImageNameAndProject_ProjectID(String imageName, ObjectId projectId);

    /**
     * Find all images associated with a project ID.
     */
    @Query("{ 'project.$id': ?0 }")
    List<Image> findAllByProject_ProjectID(ObjectId projectId);

    /**
     * Find all images associated with a project ID with pagination.
     */
    @Query("{ 'project.$id': ?0 }")
    Page<Image> findAllByProject_ProjectID(ObjectId projectId, Pageable pageable);

    /**
     * Delete all images associated with a project ID.
     */
    void deleteAllByProject_ProjectID(ObjectId projectId);

    /**
     * Find an image by its ID and project ID.
     */
    Optional<Image> findByImageIdAndProject_ProjectID(String imageId, ObjectId projectId);

    /**
     * Delete an image by its ID and project ID.
     */
    void deleteByImageIdAndProject_ProjectID(String imageId, ObjectId projectId);

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
    long countByProject_ProjectID(ObjectId projectId);

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
}