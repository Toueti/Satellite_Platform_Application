package com.enit.satellite_platform.modules.project_management.repositories;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.enit.satellite_platform.modules.project_management.entities.Project;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.User;
import com.enit.satellite_platform.shared.repository.SoftDeletableRepository;

/**
 * Repository interface for managing {@link Project} entities in MongoDB.
 * Provides methods for performing CRUD operations and custom queries on projects.
 * Extends SoftDeletableRepository to inherit soft delete query methods.
 */
@Repository
public interface ProjectRepository extends SoftDeletableRepository<Project, ObjectId> { // Extend SoftDeletableRepository

    /**
     * Finds all projects owned by a specific user email.
     *
     * @param ownerId The ObjectId of the owner.
     * @return A page of non-deleted projects owned by the user.
     */
    // Renamed to exclude soft-deleted projects
    Page<Project> findByOwnerIdAndDeletedFalse(ObjectId ownerId, Pageable pageable);

    // Keep this one for internal use if needed, or rename/remove if not used elsewhere
    List<Project> findByOwnerId(ObjectId ownerId);

    /**
     * Finds a project by its name.
     *
     * @param projectName The name of the project.
     * @return An Optional containing the project if found, or an empty Optional otherwise.
     */
    Optional<Project> findByProjectName(String projectName); // Keep for potential internal use or specific cases

    /**
     * Finds a non-deleted project by its name.
     *
     * @param projectName The name of the project.
     * @return An Optional containing the non-deleted project if found, or an empty Optional otherwise.
     */
    @Query("{ 'projectName': ?0, 'deleted': false }")
    Optional<Project> findByProjectNameAndDeletedFalse(String projectName);

    /**
     * Finds projects shared with a specific user.
     *
     * @param user The user with whom the projects are shared.
     * @return A list of projects shared with the user.
     */
    List<Project> findBySharedUsersContaining(User user);

    /**
     * Finds projects owned by a user, ordered by last accessed time in descending order.
     *
     * @param email    The email of the owner.
     * @param ownerId  The ObjectId of the owner.
     * @param pageable Pagination information.
     * @return A list of projects owned by the user, ordered by last accessed time.
     */
    List<Project> findByOwnerIdOrderByLastAccessedTimeDesc(ObjectId ownerId, Pageable pageable);

    /**
     * Finds projects shared with a user, ordered by last accessed time in descending order.
     *
     * @param user     The user with whom the projects are shared.
     * @param pageable Pagination information.
     * @return A list of projects shared with the user, ordered by last accessed time.
     */
    List<Project> findBySharedUsersContainingOrderByLastAccessedTimeDesc(User user, Pageable pageable);

    /**
     * Finds archived projects owned by a user.
     *
     * @param user The owner of the projects.
     * @return A page of archived projects owned by the user.
     */
    @Query("{ 'owner.$id': ?0, 'archived': true }")
    Page<Project> findByOwnerAndArchivedTrue(User user, Pageable pageable);

    /**
     * Searches for projects owned by a user by name or description.
     *
     * @param owner    The owner of the projects.
     * @param query    The search query.
     * @param pageable Pagination information.
     * @return A page of projects matching the search criteria.
     */
    @Query("{ 'owner.$id': ?0, $or: [ { 'projectName': { $regex: ?1, $options: 'i' } }, { 'description': { $regex: ?1, $options: 'i' } } ], 'deleted': false }")
    Page<Project> findByOwnerAndSearchCriteriaAndDeletedFalse(ObjectId ownerId, String query, Pageable pageable);

    /**
     * Finds projects owned by a user with a specific tag.
     *
     * @param owner The owner of the projects.
     * @param tag   The tag to search for.
     * @return A page of projects with the specified tag.
     */
    // Apply $regex directly for case-insensitive search within the tags set/array
    // Changed parameter from User owner to ObjectId ownerId
    @Query("{ 'owner.$id': ?0, 'tags': { $regex: ?1, $options: 'i' }, 'deleted': false }")
    Page<Project> findByOwnerAndTagsContainingAndDeletedFalse(ObjectId ownerId, String tag, Pageable pageable);

    /**
     * Finds projects owned by a user with a specific status.
     *
     * @param owner  The owner of the projects.
     * @param status The status to search for.
     * @return A page of projects with the specified status.
     */
    // Changed parameter from User owner to ObjectId ownerId
    @Query("{ 'owner.$id': ?0, 'status': ?1, 'deleted': false }")
    Page<Project> findByOwnerAndStatusAndDeletedFalse(ObjectId ownerId, String status, Pageable pageable);

    // findByDeletedTrue() is inherited or replaced by findByDeletedTrueAndDeletedAtBefore

    /**
     * Finds soft-deleted projects owned by a user with pagination.
     *
     * @param user     The owner of the projects.
     * @param pageable Pagination information.
     * @return A page of soft-deleted projects owned by the user.
     */
    // Changed parameter from User user to ObjectId ownerId
    @Query("{ 'owner.$id': ?0, 'deleted': true }")
    Page<Project> findByOwnerAndDeletedTrue(ObjectId ownerId, Pageable pageable);

    /**
     * Checks if a project exists by its ID.
     *
     * @param id The ID of the project.
     * @return True if the project exists, false otherwise.
     */
    boolean existsById(@NonNull ObjectId id);

    /**
     * Finds all projects by a list of IDs.
     *
     * @param ids The list of project IDs.
     * @return A list of projects matching the provided IDs.
     */
    @NonNull
    List<Project> findAllById(@NonNull Iterable<ObjectId> ids);

    @Query("{ 'owner.$id': ?0, 'projectName': ?1 }")
    boolean existsByProjectNameAndUserId(ObjectId id, String projectName);

    /**
     * Finds a project by its name and owner's email.
     *
     * @param projectName The name of the project.
     * @param email       The email of the owner.
     * @return An Optional containing the project if found, or an empty Optional otherwise.
     */
    @Query("{ 'owner.$id': ?0, 'projectName': ?1 }")
    Optional<Project> findByProjectNameAndUserId(ObjectId id, String projectName);

    /**
     * Finds projects where the sharedUsers map contains the given user as a key.
     *
     * @param user The user to search for in the sharedUsers map.
     * @return A list of projects shared with the user.
     */
    @Query("{'sharedUsers.?0': {$exists: true}}")
    List<Project> findBySharedUsersContainsKey(User user);

    /**
     * Finds projects where the sharedUsers map contains the given user as a key, with pagination.
     *
     * @param user The user to search for in the sharedUsers map.
     * @param pageable Pagination information.
     * @return A page of projects shared with the user.
     */
    @Query("{'sharedUsers.?0': {$exists: true}, 'deleted': false}")
    Page<Project> findBySharedUsersContainsKeyPageAndDeletedFalse(User user, Pageable pageable);

    /**
     * Counts projects where the sharedUsers map contains the given user as a key.
     *
     * @param user The user to search for in the sharedUsers map.
     * @return The count of projects shared with the user.
     */
    @Query(value = "{'sharedUsers.?0': {$exists: true}}", count = true)
    long countBySharedUsersContainsKey(User user);

    /**
     * Finds all projects owned by a specific user.
     *
     * @param ownerId The ObjectId of the owner.
     * @return A list of projects owned by the user.
     */
    List<Project> findAllByOwnerId(ObjectId ownerId);

    // Ensure the method required by the abstract cleanup service exists
    // This might already be inherited if SoftDeletableRepository defines it,
    // but adding it explicitly here for clarity if needed by the implementation.
    // If SoftDeletableRepository already defines this exact signature, this explicit declaration can be removed.
    @Override // Added Override as it's inherited
    Optional<List<Project>> findByDeletedTrueAndDeletedAtBefore(Date cutoffDate);

    /**
     * Finds a non-deleted project by its ID.
     *
     * @param id The ID of the project.
     * @return An Optional containing the non-deleted project if found, or an empty Optional otherwise.
     */
    @Query("{ '_id': ?0, 'deleted': false }")
    Optional<Project> findByIdAndDeletedFalse(ObjectId id);

}
