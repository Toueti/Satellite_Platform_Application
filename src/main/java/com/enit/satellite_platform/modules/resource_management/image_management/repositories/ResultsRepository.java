package com.enit.satellite_platform.modules.resource_management.image_management.repositories;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.enit.satellite_platform.modules.resource_management.image_management.models.ProcessingResults;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing GeeResults entities in MongoDB.
 */
@Repository
public interface ResultsRepository extends MongoRepository<ProcessingResults, ObjectId> {

    /**
     * Find all GeeResults by image ID.
     */
    @Query("{ 'image.$id': ?0 }")
    Optional<List<ProcessingResults>> findByImage_ImageId(String imageId);

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

    Optional<ProcessingResults> findByCacheKey(String cacheKey);
}
