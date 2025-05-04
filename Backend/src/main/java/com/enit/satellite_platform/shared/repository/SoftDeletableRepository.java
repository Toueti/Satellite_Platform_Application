package com.enit.satellite_platform.shared.repository;

import com.enit.satellite_platform.shared.model.SoftDeletable;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Generic repository interface for entities supporting soft deletion.
 *
 * @param <T>  The type of the entity, must implement SoftDeletable.
 * @param <ID> The type of the entity's ID.
 */
@NoRepositoryBean // Indicate that this is not a concrete repository bean itself
public interface SoftDeletableRepository<T extends SoftDeletable, ID> extends MongoRepository<T, ID> {

    /**
     * Finds all entities that are marked as deleted and were deleted before the specified cutoff date.
     *
     * @param cutoffDate The date before which entities must have been deleted.
     * @return An Optional containing a list of matching entities, or an empty Optional if none found.
     */
    Optional<List<T>> findByDeletedTrueAndDeletedAtBefore(Date cutoffDate);

    /**
     * Finds an entity by its ID only if it is not marked as deleted.
     *
     * @param id The ID of the entity.
     * @return An Optional containing the non-deleted entity, or empty if not found or deleted.
     */
    @Query("{ '_id': ?0, 'deleted': false }")
    Optional<T> findByIdAndDeletedFalse(ID id);

    /**
     * Finds all entities that are not marked as deleted.
     *
     * @return An Optional containing a list of non-deleted entities, or empty if none found.
     */
    @Query("{ 'deleted': false }")
    Optional<List<T>> findByDeletedFalse();

    /**
     * Finds all entities that are not marked as deleted, with pagination.
     *
     * @param pageable Pagination information.
     * @return A Page containing non-deleted entities.
     */
    @Query("{ 'deleted': false }")
    Page<T> findByDeletedFalse(Pageable pageable);

    /**
     * Finds all entities that are marked as deleted.
     *
     * @return An Optional containing a list of deleted entities, or empty if none found.
     */
    @Query("{ 'deleted': true }")
    Optional<List<T>> findByDeletedTrue();

    /**
     * Finds all entities that are marked as deleted, with pagination.
     *
     * @param pageable Pagination information.
     * @return A Page containing deleted entities.
     */
    @Query("{ 'deleted': true }")
    Page<T> findByDeletedTrue(Pageable pageable);

}
