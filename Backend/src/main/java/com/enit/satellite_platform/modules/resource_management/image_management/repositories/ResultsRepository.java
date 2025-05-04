package com.enit.satellite_platform.modules.resource_management.image_management.repositories;


import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.enit.satellite_platform.modules.resource_management.image_management.entities.Image; // Import Image
import com.enit.satellite_platform.modules.resource_management.image_management.entities.ProcessingResults;
import com.enit.satellite_platform.shared.repository.SoftDeletableRepository; // Import SoftDeletableRepository

/**
 * Repository for managing ProcessingResults entities in MongoDB.
 * Extends SoftDeletableRepository to inherit soft delete query methods.
 */
@Repository
public interface ResultsRepository extends SoftDeletableRepository<ProcessingResults, ObjectId>, ResultsRepositoryCustom { // Extend SoftDeletableRepository and Custom Repo

    /**
     * Find all GeeResults by image ID.
     */
    @Query("{ 'image.$id': ?0 }")
    Optional<List<ProcessingResults>> findByImage_ImageId(String imageId); // Keep existing for potential other uses, though likely incorrect for DBRef

    // REMOVED: Optional<List<ProcessingResults>> findByImage_ImageId(ObjectId imageId);

    /**
     * Find all GeeResults by the referenced Image object.
     * Spring Data should generate the correct query for the DBRef.
     */
    Optional<List<ProcessingResults>> findByImage(Image image); // Add method accepting Image object

    /**
     * Find all non-deleted GeeResults by the referenced Image object.
     */
    Optional<List<ProcessingResults>> findByImageAndDeletedFalse(Image image);

    /**
     * Delete all GeeResults by image ID.
     */
    @Transactional
    @Query(value = "{ 'image.$id': ?0 }", delete = true)
    void deleteAllByImage_ImageId(String imageId);

    /**
     * Check if GeeResults exist for an image ID.
     */
    @Query(value = "{ 'image.$id': ?0 }", exists = true)
    boolean existsByImage_ImageId(String imageId);

    /**
     * Delete a specific GeeResults by image ID and GeeResults ID.
     */
    @Transactional
    @Query(value = "{ '_id': ?1, 'image.$id': ?0 }", delete = true)
    void deleteByImage_ImageIdAndId(String imageId, ObjectId id);

    /**
     * Count GeeResults by image ID.
     */
    @Query(value = "{ 'image.$id': ?0 }", count = true)
    long countByImage_ImageId(String imageId);

    /**
     * Check if a specific GeeResults exists for an image ID and GeeResults ID.
     */
    @Query(value = "{ '_id': ?1, 'image.$id': ?0 }", exists = true)
    boolean existsByImage_ImageIdAndResultsId(String imageId, ObjectId resultsId);

    /**
     * Find all processing results where the associated image's project's owner ID matches the given user ID.
     * Note: Assumes owner ID in Project is stored as ObjectId.
     *
     * @param ownerId The ObjectId of the owner user, as a String.
     * @return A list of processing results owned by the user.
     */
    @Query("{ 'image.project.owner.$id' : ?0 }") // Query based on the referenced image's project's owner's ID
    List<ProcessingResults> findAllByOwnerId(String ownerId);

    /**
     * Find all non-deleted GeeResults by image ID.
     */
    @Query("{ 'image.$id': ?0, 'deleted': false }")
    Optional<List<ProcessingResults>> findByImage_ImageIdAndDeletedFalse(String imageId);

    /**
     * Find a specific GeeResults by image ID and GeeResults ID.
     */
    @Query("{ '_id': ?1, 'image.$id': ?0 }")
    Optional<ProcessingResults> findByImage_ImageIdAndResultsId(String imageId, ObjectId resultsId);

    /**
     * Find all soft-deleted results with pagination.
     */
    Page<ProcessingResults> findByDeletedTrue(Pageable pageable);

     /**
     * Find all soft-deleted results deleted before a certain date.
     */
    // Method already added in a previous step, signature matches SoftDeletableRepository
    @Override // Add Override if inheriting, otherwise remove
    Optional<List<ProcessingResults>> findByDeletedTrueAndDeletedAtBefore(java.util.Date cutoffDate);

    /**
     * Find all processing results associated with a specific project ID.
     *
     * @param projectId The ID of the project.
     * @return A list of processing results for the given project.
     */
    @Query("{ 'image.project.$id' : ?0 }")
    List<ProcessingResults> findByProjectId(String projectId);

    /**
     * Find all non-deleted processing results associated with a specific project ID.
     *
     * @param projectId The ID of the project.
     * @return A list of non-deleted processing results for the given project.
     */
    @Query("{ 'image.project.$id' : ?0, 'deleted': false }")
    List<ProcessingResults> findByProjectIdAndDeletedFalse(String projectId);
}
