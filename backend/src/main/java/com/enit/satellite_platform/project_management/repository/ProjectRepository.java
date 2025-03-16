package com.enit.satellite_platform.project_management.repository;

import com.enit.satellite_platform.project_management.model.Project;
import com.enit.satellite_platform.user_management.model.User;
import org.bson.types.ObjectId;

import org.springframework.lang.NonNull;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link Project} entities in MongoDB.
 * Provides methods for performing CRUD operations and custom queries on projects.
 */
@Repository
public interface ProjectRepository extends MongoRepository<Project, ObjectId> {

    /**
     * Finds all projects owned by a specific user.
     *
     * @param user The owner of the projects.
     * @return A list of projects owned by the user.
     */
    List<Project> findByOwner(User user);

    /**
     * Finds a project by its name.
     *
     * @param projectName The name of the project.
     * @return An Optional containing the project if found, or an empty Optional otherwise.
     */
    Optional<Project> findByProjectName(String projectName);

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
     * @param pageable Pagination information.
     * @return A list of projects owned by the user, ordered by last accessed time.
     */
    List<Project> findByOwner_EmailOrderByLastAccessedTimeDesc(String email, Pageable pageable);

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
     * @return A list of archived projects owned by the user.
     */
    @Query("{ 'owner.$id': ?0, 'archived': true }")
    List<Project> findByOwnerAndArchivedTrue(User user);

    /**
     * Searches for projects owned by a user by name or description.
     *
     * @param owner    The owner of the projects.
     * @param query    The search query.
     * @param pageable Pagination information.
     * @return A list of projects matching the search criteria.
     */
    @Query("{ 'owner.$id': ?0, $or: [ { 'projectName': { $regex: ?1, $options: 'i' } }, { 'description': { $regex: ?1, $options: 'i' } } ] }")
    List<Project> findByOwnerAndSearchCriteria(User owner, String query, Pageable pageable);

    /**
     * Finds projects owned by a user with a specific tag.
     *
     * @param owner The owner of the projects.
     * @param tag   The tag to search for.
     * @return A list of projects with the specified tag.
     */
    @Query("{ 'owner.$id': ?0, 'tags': ?1 }")
    List<Project> findByOwnerAndTagsContaining(User owner, String tag);

    /**
     * Finds projects owned by a user with a specific status.
     *
     * @param owner  The owner of the projects.
     * @param status The status to search for.
     * @return A list of projects with the specified status.
     */
    @Query("{ 'owner.$id': ?0, 'status': ?1 }")
    List<Project> findByOwnerAndStatus(User owner, String status);

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
    Optional<Project> findByProjectNameAndUserId(ObjectId id, String projectName);;
}
