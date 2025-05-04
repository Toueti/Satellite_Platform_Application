package com.enit.satellite_platform.modules.project_management.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.enit.satellite_platform.exceptions.DuplicationException;
import com.enit.satellite_platform.modules.project_management.dto.ProjectDto;
import com.enit.satellite_platform.modules.project_management.dto.ProjectStatisticsDto;
import com.enit.satellite_platform.modules.project_management.dto.SharedUserInfoDto; // Added import
import com.enit.satellite_platform.modules.project_management.entities.PermissionLevel;
import com.enit.satellite_platform.modules.project_management.entities.Project;
import com.enit.satellite_platform.modules.project_management.exceptions.ProjectNotFoundException;
import com.enit.satellite_platform.modules.project_management.repositories.ProjectRepository;
import com.enit.satellite_platform.modules.resource_management.image_management.repositories.ImageRepository;
import com.enit.satellite_platform.modules.resource_management.image_management.services.ImageService;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.User;
import com.enit.satellite_platform.modules.user_management.normal_user_service.repositories.UserRepository;
import com.enit.satellite_platform.shared.mapper.ProjectMapper;

/**
 * Service class for managing projects.
 * Provides methods for creating, retrieving, updating, deleting, sharing, and
 * performing other operations on projects.
 */
@Service
@RefreshScope
public class ProjectService {

  private static final Logger logger = LoggerFactory.getLogger(ProjectService.class);

  /**
   * The repository for managing Project entities.
   */
  @Autowired
  private ProjectRepository projectRepository;

  /**
   * The repository for managing Image entities.
   */
  @Autowired
  private ImageRepository imageRepository;

  /**
   * The repository for managing User entities.
   */
  @Autowired
  private UserRepository userRepository;

  @Value("${project.base.path}")
  private String projectBasePath;

  /**
   * Service for managing image-related operations.
   */
  @Autowired
  private ImageService imageService;

  @Autowired
  private ProjectMapper projectMapper;

  /**
   * Creates a new project.
   *
   * @param project The project to create.
   * @param email   The email of the user creating the project.
   * @return The created project as ProjectDTO.
   */
  @Transactional
  public Project createProject(Project project, String email) {
    logger.info("Creating project for email: {}", email);
    validateProject(project);
    User thematician = getUserByEmail(email, "Thematician not found");
    project.setOwner(thematician);
    project.updateLastAccessedTime();

    try {
      Project savedProject = projectRepository.save(project);

      logger.info("Project created successfully with ID: {}", savedProject.getId());

      return savedProject;
    } catch (DataIntegrityViolationException e) {
      logger.error("Duplicate project name for user: {}", email, e);
      throw new DuplicationException("A project with the same name already exists for this user.");
    } catch (Exception e) {
      logger.error("Failed to create project", e);
      throw new RuntimeException("Failed to create project: " + e.getMessage(), e);
    }
  }

  @Transactional
  public ProjectDto renameProject(ObjectId projectId, String newName, String email) {
    logger.info("Renaming project with ID: {} to new name: {} for user: {}", projectId, newName, email);

    Project project = projectRepository.findById(projectId)
        .orElseThrow(() -> {
          logger.error("Project not found with ID: {}", projectId);
          return new IllegalArgumentException("Project not found with ID: " + projectId);
        });

    User owner = project.getOwner();
    // Check if the project belongs to the user
    if (!owner.getEmail().equals(email)) {
      logger.warn("User {} does not own project {}", email, projectId);
      throw new IllegalArgumentException("You do not have permission to rename this project.");
    }

    // Check for duplicate name for the same user (excluding this project)
    Optional<Project> existingProject = projectRepository.findByProjectNameAndUserId(new ObjectId(owner.getId()),
        newName);
    if (existingProject.isPresent() && !existingProject.get().getId().equals(projectId.toString())) {
      logger.warn("Project name '{}' already exists for user '{}'", newName, email);
      throw new DuplicationException("A project with the name '" + newName + "' already exists for this user.");
    }

    try {
      project.setProjectName(newName);
      Project updatedProject = projectRepository.save(project);
      logger.info("Project renamed successfully to: {}", newName);
      return projectMapper.toDTO(updatedProject);
    } catch (DataIntegrityViolationException e) {
      logger.warn("Duplicate project name '{}' for user '{}'", newName, email);
      throw new DuplicationException("A project with the name '" + newName + "' already exists for this user.");
    }
  }

  /**
   * Retrieves a project by its ID.
   *
   * @param id The ID of the project to retrieve.
   * @return The project with the given ID as ProjectDTO.
   * @throws ProjectNotFoundException If no project with the given ID is found.
   */
  public ProjectDto getProject(ObjectId id) {
    return projectMapper.toDTO(getProjectById(id));
  }

  private Project getProjectById(ObjectId id) {
    logger.info("Fetching project with ID: {}", id);
    validateObjectId(id, "Project ID");
    // Use findByIdAndDeletedFalse to exclude soft-deleted projects
    Project project = projectRepository.findByIdAndDeletedFalse(id)
        .orElseThrow(() -> {
          logger.error("Project not found with ID: {} (or it is deleted)", id);
          return new ProjectNotFoundException("Project not found with ID: " + id + " (or it is deleted)");
        });
    project.setLastAccessedTime(new Date());
    projectRepository.save(project);
    return project;
  }

  /**
   * Retrieves a project by its name.
   *
   * @param projectName The name of the project to retrieve.
   * @return The project with the given name as ProjectDTO.
   * @throws ProjectNotFoundException If no project with the given name is found.
   */
  public ProjectDto getProjectByName(String projectName) {
    logger.info("Fetching project with name: {}", projectName);
    // Use findByProjectNameAndDeletedFalse to exclude soft-deleted projects
    Project project = projectRepository.findByProjectNameAndDeletedFalse(projectName)
        .orElseThrow(() -> {
          logger.error("Project not found with name: {} (or it is deleted)", projectName);
          return new ProjectNotFoundException("Project not found with name: " + projectName + " (or it is deleted)");
        });
    project.setLastAccessedTime(new Date());
    projectRepository.save(project);
    return projectMapper.toDTO(project);
  }

  /**
   * Retrieves statistics for all projects owned by a user.
   *
   * @param email The email of the user.
   * @return A ProjectStatisticsDto object containing the statistics.
   * @throws UsernameNotFoundException If the user with the given email is not
   *                                   found.
   */
  public ProjectStatisticsDto getStatistics(String email) {
    logger.info("Fetching statistics for email: {}", email);
    User owner = getUserByEmail(email, "User not found for statistics: " + email);
    List<Project> allProjects = projectRepository.findAllByOwnerId(new ObjectId(owner.getId()));
    long totalProjects = allProjects.size();
    if (totalProjects == 0) {
      logger.warn("No projects found for email: {}", email);
    }
    Map<ObjectId, Long> imagesPerProject = new HashMap<>();
    Map<ObjectId, Date> projectTimeIntervals = new HashMap<>();

    for (Project project : allProjects) {
      long imageCount = imageRepository.countByProject(project);
      imagesPerProject.put(new ObjectId(project.getId()), imageCount);
      projectTimeIntervals.put(new ObjectId(project.getId()), project.getLastAccessedTime());
    }

    return new ProjectStatisticsDto(totalProjects, imagesPerProject, projectTimeIntervals);
  }

  /**
   * Retrieves all projects owned by a user.
   *
   * @param email The email of the user.
   * @return A list of projects owned by the user.
   * @throws UsernameNotFoundException If the user with the given email is not
   *                                   found.
   */
  public Page<ProjectDto> getAllProjects(String email, Pageable pageable) {
    logger.info("Fetching all projects for email: {}", email);
    validatePageable(pageable);
    User owner = getUserByEmail(email, "User not found for fetching projects: " + email);
    // Use the renamed method to exclude soft-deleted projects
    Page<Project> projects = projectRepository.findByOwnerIdAndDeletedFalse(new ObjectId(owner.getId()), pageable);
    if (projects.isEmpty()) {
      logger.warn("No non-deleted projects found for email: {}", email); // Updated log message
      throw new ProjectNotFoundException("No projects found for user: " + email);
    }
    return projectMapper.toDTOPage(projects);
  }

  /**
   * Updates an existing project.
   *
   * @param projectId The ID of the project to update.
   * @param project   The updated project data.
   * @return The updated project as ProjectDTO.
   * @throws ProjectNotFoundException If no project with the given ID is found.
   * @throws IllegalArgumentException If the project data is invalid.
   * @throws RuntimeException         If an unexpected error occurs during project
   *                                  update.
   */
  @Transactional
  public ProjectDto updateProject(ObjectId projectId, Project project) {
    logger.info("Updating project with ID: {}", projectId);
    validateObjectId(projectId, "Project ID");
    validateProject(project);
    Project existingProject = getProjectById(projectId);
    existingProject.setProjectName(project.getProjectName());
    existingProject.setDescription(project.getDescription());
    existingProject.setImages(project.getImages());
    existingProject.setUpdatedAt(new Date());
    try {
      Project updatedProject = projectRepository.save(existingProject);
      logger.info("Project updated successfully with ID: {}", projectId);
      return projectMapper.toDTO(updatedProject);
    } catch (Exception e) {
      logger.error("Failed to update project with ID: {}", projectId, e);
      throw new RuntimeException("Failed to update project: " + e.getMessage(), e);
    }
  }

  /**
   * Deletes a project by its ID.
   * This method also deletes all images associated with the project.
   *
   * @param projectId The ID of the project to delete.
   * @throws ProjectNotFoundException If no project with the given ID is found.
   * @throws RuntimeException         If an unexpected error occurs during project
   *                                  deletion.
   */
  @Transactional
  public void deleteProject(ObjectId projectId) {
    logger.info("Attempting to soft delete project with ID: {}", projectId);
    validateObjectId(projectId, "Project ID");
    Project project = getProjectById(projectId); // Fetch project first

    try {
      project.setDeleted(true);
      project.setDeletedAt(new Date());
      projectRepository.save(project);
      logger.info("Project soft deleted successfully with ID: {}", projectId);

      // Cascade soft delete to associated images
      imageService.softDeleteAllImagesByProject(projectId);

    } catch (Exception e) {
      logger.error("Failed to soft delete project with ID: {}", projectId, e);
      throw new RuntimeException("Failed to soft delete project: " + e.getMessage(), e);
    }
  }

  /**
   * Permanently deletes a project by its ID.
   * This method also deletes all images associated with the project.
   * Intended to be called by the cleanup service.
   *
   * @param projectId The ID of the project to permanently delete.
   * @throws ProjectNotFoundException If no project with the given ID is found.
   * @throws RuntimeException         If an unexpected error occurs during project
   *                                  deletion.
   */
  @Transactional
  public void permanentlyDeleteProject(ObjectId projectId) {
    logger.info("Attempting to permanently delete project with ID: {}", projectId);
    validateObjectId(projectId, "Project ID");
    Project project = projectRepository.findById(projectId)
        .orElseThrow(() -> {
          logger.error("Project not found for permanent deletion with ID: {}", projectId);
          // If not found, it might have been deleted already, log and return
          return new ProjectNotFoundException("Project not found with ID: " + projectId);
        });

    try {
      // Permanently delete all images and their GEE data via ImageService
      imageService.permanentlyDeleteAllImagesByProject(projectId);

      // Remove project reference from owner's list
      User owner = project.getOwner();
      if (owner != null) {
        owner.getProjects().removeIf(p -> p.getId().equals(projectId.toString()));
        userRepository.save(owner);
      }

      // Remove project reference from shared users' lists
      project.getSharedUsers().keySet().forEach(userId -> {
        userRepository.findById(userId).ifPresent(sharedUser -> {
          sharedUser.getSharedProjects().removeIf(p -> p.getId().equals(projectId.toString()));
          userRepository.save(sharedUser);
        });
      });

      projectRepository.deleteById(projectId);
      logger.info("Project and associated data permanently deleted successfully with ID: {}", projectId);

    } catch (Exception e) {
      logger.error("Failed to permanently delete project with ID: {}", projectId, e);
      throw new RuntimeException("Failed to permanently delete project: " + e.getMessage(), e);
    }
  }

  /**
   * Restores a soft-deleted project.
   *
   * @param projectId The ID of the project to restore.
   * @param email     The email of the user performing the action.
   * @return The restored project as ProjectDTO.
   * @throws ProjectNotFoundException If the project is not found or not deleted.
   * @throws AccessDeniedException    If the user is not the owner.
   */
  @Transactional
  public ProjectDto restoreProject(ObjectId projectId, String email) {
    logger.info("Restoring project with ID: {} by email: {}", projectId, email);
    Project project = projectRepository.findById(projectId)
        .orElseThrow(() -> new ProjectNotFoundException("Project not found with ID: " + projectId));

    validateOwner(project, email, "restore");

    if (!project.isDeleted()) {
      throw new IllegalStateException("Project is not deleted.");
    }

    project.setDeleted(false);
    project.setDeletedAt(null);
    project.setRetentionDays(null); // Reset retention days on restore
    Project restoredProject = projectRepository.save(project);
    logger.info("Project restored successfully: {}", projectId);

    // Cascade restore to associated images
    imageService.restoreAllImagesByProject(projectId);

    return projectMapper.toDTO(restoredProject);
  }

  /**
   * Updates the retention period for a soft-deleted project.
   *
   * @param projectId     The ID of the project.
   * @param retentionDays The new retention period in days.
   * @param email         The email of the user performing the action.
   * @return The updated project as ProjectDTO.
   * @throws ProjectNotFoundException If the project is not found or not deleted.
   * @throws AccessDeniedException    If the user is not the owner.
   * @throws IllegalArgumentException If retentionDays is negative.
   */
  @Transactional
  public ProjectDto updateRetentionPeriod(ObjectId projectId, int retentionDays, String email) {
    logger.info("Updating retention period for project ID: {} to {} days by email: {}", projectId, retentionDays,
        email);
    if (retentionDays < 0) {
      throw new IllegalArgumentException("Retention period cannot be negative.");
    }

    Project project = projectRepository.findById(projectId)
        .orElseThrow(() -> new ProjectNotFoundException("Project not found with ID: " + projectId));

    validateOwner(project, email, "update retention period");

    if (!project.isDeleted()) {
      throw new IllegalStateException("Cannot update retention period for a project that is not deleted.");
    }

    project.setRetentionDays(retentionDays);
    Project updatedProject = projectRepository.save(project);
    logger.info("Retention period updated successfully for project: {}", projectId);
    return projectMapper.toDTO(updatedProject);
  }

  /**
   * Forces the permanent deletion of a soft-deleted project, bypassing retention.
   *
   * @param projectId The ID of the project to force delete.
   * @param email     The email of the user performing the action (must be owner).
   * @throws ProjectNotFoundException If the project is not found.
   * @throws AccessDeniedException    If the user is not the owner.
   * @throws IllegalStateException    If the project is not soft-deleted.
   */
  @Transactional
  public void forceDeleteProject(ObjectId projectId, String email) {
    logger.info("Force deleting project ID: {} by email: {}", projectId, email);
    Project project = projectRepository.findById(projectId)
        .orElseThrow(() -> new ProjectNotFoundException("Project not found with ID: " + projectId));

    validateOwner(project, email, "force delete");

    if (!project.isDeleted()) {
      throw new IllegalStateException("Project is not soft-deleted. Cannot force delete.");
    }

    // Call the existing permanent delete logic
    permanentlyDeleteProject(projectId);
    logger.info("Project force deleted successfully: {}", projectId);
  }

  /**
   * Shares a project with another user.
   *
   * @param projectId    The ID of the project.
   * @param otherEmail   The email of the user to share with.
   * @param currentEmail The email of the current user (project owner).
   * @param permission   The permission level to grant.
   * @return The updated project as ProjectDTO after sharing.
   * @throws ProjectNotFoundException  If the project with the given ID is not
   *                                   found.
   * @throws UsernameNotFoundException If the user to share with is not found.
   * @throws AccessDeniedException     If the current user is not the owner of the
   *                                   project.
   * @throws IllegalArgumentException  If the sharing request is invalid.
   */
  @Transactional
  public SharedUserInfoDto shareProject(String projectId, String otherEmail, String currentEmail,
      PermissionLevel permission) {
    logger.info("Sharing project with ID: {} with permission {} by email: {}", projectId, permission, currentEmail);
    Project project = getProjectById(new ObjectId(projectId));
    validateOwner(project, currentEmail, "share");

    User userToShare = getUserByEmail(otherEmail, "User not found with email: " + otherEmail);
    project.shareWith(userToShare, permission);
    projectRepository.save(project);
    userToShare.getSharedProjects().add(project);
    userRepository.save(userToShare);
    logger.info("Project shared successfully with user: {}", otherEmail);

    // Return SharedUserInfoDto instead of ProjectDto
    return new SharedUserInfoDto(userToShare.getId(), userToShare.getName(), userToShare.getEmail(), permission);
  }

  /**
   * Unshares a project with another user.
   *
   * @param projectId    The ID of the project.
   * @param otherEmail   The email of the user to unshare with.
   * @param currentEmail The email of the current user (project owner).
   * @return The updated project as ProjectDTO after unsharing.
   * @throws ProjectNotFoundException  If the project with the given ID is not
   *                                   found.
   * @throws UsernameNotFoundException If the user to unshare with is not found.
   * @throws AccessDeniedException     If the current user is not the owner of the
   *                                   project.
   * @throws IllegalArgumentException  If the sharing request is invalid.
   */
  @Transactional
  public SharedUserInfoDto unshareProject(String projectId, String otherEmail, String currentEmail) {
    logger.info("Unsharing project with ID: {} by email: {}", projectId, currentEmail);
    Project project = getProjectById(new ObjectId(projectId));
    validateOwner(project, currentEmail, "unshare");

    User userToUnshare = getUserByEmail(otherEmail, "User not found with email: " + otherEmail);
    project.unshareWith(userToUnshare);
    projectRepository.save(project);
    userToUnshare.getSharedProjects().remove(project);
    userRepository.save(userToUnshare);
    logger.info("Project unshared successfully with user: {}", otherEmail);

    // Return SharedUserInfoDto instead of ProjectDto (permissionLevel is null as
    // it's irrelevant for unshare)
    return new SharedUserInfoDto(userToUnshare.getId(), userToUnshare.getName(), userToUnshare.getEmail(), null);
  }

  /**
   * Retrieves the users with whom a project is shared.
   *
   * @param projectId    The ID of the project.
   * @param currentEmail The email of the current user.
   * @return A set of users with whom the project is shared.
   * @throws ProjectNotFoundException  If the project with the given ID is not
   *                                   found.
   * @throws UsernameNotFoundException If the current user is not found.
   * @throws AccessDeniedException     If the current user does not have access to
   *                                   view the shared users.
   */
  public Set<SharedUserInfoDto> getSharedUsers(ObjectId projectId, String currentEmail) {
    logger.info("Fetching shared users with permissions for project ID: {} by email: {}", projectId, currentEmail);
    validateObjectId(projectId, "Project ID");
    Project project = getProjectById(projectId);
    User currentUser = getUserByEmail(currentEmail, "Current user not found");

    if (!project.hasAccess(currentUser)) {
      logger.error("Access denied for email: {} to view shared users of project: {}", currentEmail, projectId);
      throw new AccessDeniedException("Access denied to view shared users");
    }

    Map<ObjectId, PermissionLevel> sharedUsersMap = project.getSharedUsers();
    Set<ObjectId> sharedUserIds = sharedUsersMap.keySet();

    if (sharedUserIds.isEmpty()) {
      return Collections.emptySet();
    }

    List<User> users = userRepository.findAllById(sharedUserIds);
    Set<SharedUserInfoDto> sharedUserInfoDtos = new HashSet<>();

    for (User user : users) {
      ObjectId userId = new ObjectId(user.getId()); // Convert String ID back to ObjectId for map lookup
      PermissionLevel permission = sharedUsersMap.get(userId);
      if (permission != null) { // Ensure the user is actually in the map (should always be true here)
        sharedUserInfoDtos.add(new SharedUserInfoDto(user.getId(), user.getName(), user.getEmail(), permission));
      } else {
        // Log a warning if a user fetched by ID isn't in the project's shared map -
        // indicates potential inconsistency
        logger.warn("User with ID {} found in repository but not in project {} shared map.", user.getId(), projectId);
      }
    }

    return sharedUserInfoDtos;
  }

  /**
   * Retrieves the projects shared with a specific user.
   *
   * @param email The email of the user.
   * @return A list of projects shared with the user.
   * @throws UsernameNotFoundException If the user with the given email is not
   *                                   found.
   */
  public Page<ProjectDto> getSharedWithMe(String email, Pageable pageable) {
    logger.info("Fetching projects shared with email: {}", email);
    validatePageable(pageable);
    User user = getUserByEmail(email, "User not found");
    // Use findBySharedUsersContainsKeyPageAndDeletedFalse to exclude soft-deleted projects
    Page<Project> sharedProjects = projectRepository.findBySharedUsersContainsKeyPageAndDeletedFalse(user, pageable);
    return projectMapper.toDTOPage(sharedProjects);
  }

  /**
   * Retrieves the last n accessed projects for a user, ordered by last accessed
   * time (most recent first).
   * This includes both projects owned by the user and projects shared with the
   * user.
   *
   * @param email The email of the user.
   * @param n     The number of projects to retrieve.
   * @return A list of the last n accessed projects.
   * @throws IllegalArgumentException  If n is not positive.
   * @throws UsernameNotFoundException If the user with the given email is not
   *                                   found.
   */
  // Change return type to List<ProjectDto>
  public List<ProjectDto> getLastAccessedProjects(String email, int n) {
    logger.info("Fetching last accessed projects for email: {}, limit: {}", email, n);
    if (n <= 0) {
      logger.error("Limit must be positive: {}", n);
      throw new IllegalArgumentException("Limit must be positive");
    }
    User owner = getUserByEmail(email, "User not found"); // Get the owner user first
    Pageable pageable = PageRequest.of(0, n);
    // Use the new method with owner's ObjectId
    List<Project> projects = projectRepository.findByOwnerIdOrderByLastAccessedTimeDesc(new ObjectId(owner.getId()),
        pageable);

    if (projects.size() < n) {
      // User object is already fetched as 'owner'
      // Use the 'owner' variable here instead of the undefined 'user'
      List<Project> sharedProjects = projectRepository.findBySharedUsersContainingOrderByLastAccessedTimeDesc(owner,
          pageable);
      Set<Project> combinedProjects = new LinkedHashSet<>(projects);
      combinedProjects.addAll(sharedProjects);
      projects = new ArrayList<>(combinedProjects);
    }
    // Map the final list to DTOs before returning
    List<Project> finalProjects = projects.subList(0, Math.min(projects.size(), n));
    return projectMapper.toDTOList(finalProjects);
  }

  /**
   * Archives a project.
   *
   * @param projectId The ID of the project to archive.
   * @param email     The email of the user performing the action.
   * @return The archived project as ProjectDTO.
   * @throws ProjectNotFoundException If the project with the given ID is not
   *                                  found.
   * @throws AccessDeniedException    If the user is not the owner of the project.
   */
  @Transactional
  public ProjectDto archiveProject(ObjectId projectId, String email) {
    logger.info("Archiving project with ID: {} by email: {}", projectId, email);
    Project project = getProjectById(projectId);
    validateOwner(project, email, "archive");
    project.setArchived(true);
    project.setArchivedDate(new Date());
    return projectMapper.toDTO(projectRepository.save(project));
  }

  @Transactional
  public ProjectDto unarchiveProject(ObjectId projectId, String email) {
    logger.info("Unarchiving project with ID: {} by email: {}", projectId, email);
    Project project = getProjectById(projectId);
    validateOwner(project, email, "unarchive");
    if (!project.isArchived()) {
      logger.error("Project is not archived: {}", projectId);
      throw new IllegalStateException("Project is not archived");
    }
    project.setArchived(false);
    project.setArchivedDate(null);
    return projectMapper.toDTO(projectRepository.save(project));
  }

  public Page<ProjectDto> getArchivedProjects(String email, Pageable pageable) {
    logger.info("Fetching archived projects for email: {}", email);
    validatePageable(pageable);
    User user = getUserByEmail(email, "User not found: " + email);
    Page<Project> projects = projectRepository.findByOwnerAndArchivedTrue(user, pageable);
    return projectMapper.toDTOPage(projects);
  }

  /**
   * Searches for projects owned by a user based on a query.
   *
   * @param email    The email of the user.
   * @param query    The search query to filter projects by name or description.
   * @param pageable Pagination information.
   * @return A page of projects matching the search criteria.
   * @throws UsernameNotFoundException If the user with the given email is not
   *                                   found.
   */
  public Page<ProjectDto> searchProjects(String email, String query, Pageable pageable) {
    logger.info("Searching projects for email: {} with query: {}, page: {}, size: {}", email, query,
        pageable.getPageNumber(), pageable.getPageSize());
    validatePageable(pageable);
    User user = getUserByEmail(email, "User not found: " + email);
    // validatePageable is implicitly handled by Pageable
    // The repository method already returns List, need to convert to Page manually
    // or change repo
    // Use findByOwnerAndSearchCriteriaAndDeletedFalse to exclude soft-deleted projects
    Page<Project> projects = projectRepository.findByOwnerAndSearchCriteriaAndDeletedFalse(new ObjectId(user.getId()), query, pageable);
    return projectMapper.toDTOPage(projects);
  }

  @Transactional
  public ProjectDto tagProject(ObjectId projectId, String tag, String email) {
    logger.info("Adding tag: {} to project ID: {} by email: {}", tag, projectId, email);
    validateString(tag, "Tag");
    Project project = getProjectById(projectId);
    validateOwner(project, email, "add tags");
    project.getTags().add(tag);
    return projectMapper.toDTO(projectRepository.save(project));
  }

  public Page<ProjectDto> getProjectsByTag(String email, String tag, Pageable pageable) {
    logger.info("Fetching projects by tag: {} for email: {}", tag, email);
    validatePageable(pageable);
    validateString(tag, "Tag");
    User user = getUserByEmail(email, "User not found: " + email);
    // Use findByOwnerAndTagsContainingAndDeletedFalse to exclude soft-deleted projects
    Page<Project> projects = projectRepository.findByOwnerAndTagsContainingAndDeletedFalse(new ObjectId(user.getId()), tag, pageable);
    return projectMapper.toDTOPage(projects);
  }

  @Transactional
  public ProjectDto duplicateProject(ObjectId projectId, String newName, String email) {
    logger.info("Duplicating project ID: {} with new name: {} by email: {}", projectId, newName, email);
    validateString(newName, "New project name");
    Project original = getProjectById(projectId);
    User user = validateOwner(original, email, "duplicate");
    Project duplicate = Project.builder()
        .projectName(newName)
        .description(original.getDescription())
        .owner(user)
        .images(new HashSet<>(original.getImages()))
        .sharedUsers(new HashMap<>())
        .createdAt(new Date())
        .updatedAt(new Date())
        .lastAccessedTime(new Date())
        .build();
    try {
      return projectMapper.toDTO(projectRepository.save(duplicate));
    } catch (DuplicateKeyException e) {
      logger.error("Duplicate project name: {}", newName, e);
      throw new IllegalArgumentException("A project with the name '" + newName + "' already exists.");
    }
  }

  @Transactional
  public ProjectDto updateProjectStatus(ObjectId projectId, String status, String email) {
    logger.info("Updating status of project ID: {} to: {} by email: {}", projectId, status, email);
    validateString(status, "Status");
    Project project = getProjectById(projectId);
    validateOwner(project, email, "update status");
    project.setStatus(status);
    return projectMapper.toDTO(projectRepository.save(project));
  }

  public Page<ProjectDto> getProjectsByStatus(String email, String status, Pageable pageable) {
    logger.info("Fetching projects by status: {} for email: {}", status, email);
    validatePageable(pageable);
    validateString(status, "Status");
    User user = getUserByEmail(email, "User not found: " + email);
    // Use findByOwnerAndStatusAndDeletedFalse to exclude soft-deleted projects
    Page<Project> projects = projectRepository.findByOwnerAndStatusAndDeletedFalse(new ObjectId(user.getId()), status, pageable);
    return projectMapper.toDTOPage(projects);
  }

  public Map<String, Object> exportProject(ObjectId projectId, String email) {
    // TODO update this to return a TXT file or something
    logger.info("Exporting project ID: {} for email: {}", projectId, email);
    Project project = getProjectById(projectId);
    User user = getUserByEmail(email, "User not found: " + email);
    if (!project.hasAccess(user)) {
      logger.error("Access denied for email: {} to export project: {}", email, projectId);
      throw new AccessDeniedException("User does not have access to export this project");
    }
    Map<String, Object> exportData = new HashMap<>();
    exportData.put("projectId", project.getId().toString());
    exportData.put("name", project.getProjectName());
    exportData.put("description", project.getDescription());
    exportData.put("owner", project.getOwner().getEmail());
    // Fetch User objects first, then map to emails
    Set<ObjectId> sharedUserIdsForExport = project.getSharedUsers().keySet();
    List<String> sharedUserEmails = userRepository.findAllById(sharedUserIdsForExport).stream()
        .map(User::getEmail)
        .toList();
    exportData.put("sharedUsers", sharedUserEmails);
    exportData.put("imageCount", imageRepository.countByProject(project));
    exportData.put("lastAccessed", project.getLastAccessedTime());
    return exportData;
  }

  @Transactional
  public void bulkDeleteProjects(List<ObjectId> projectIds, String email) {
    logger.info("Bulk deleting projects with IDs: {} by email: {}", projectIds, email);
    if (projectIds == null || projectIds.isEmpty()) {
      logger.error("Project IDs list is null or empty");
      throw new IllegalArgumentException("Project IDs list cannot be null or empty");
    }
    User user = getUserByEmail(email, "User not found: " + email);
    List<Project> projects = projectRepository.findAllById(projectIds);
    for (Project project : projects) {
      if (!project.getOwner().equals(user)) {
        logger.error("User {} does not own project: {}", email, project.getId());
        throw new AccessDeniedException("User does not own project: " + project.getId());
      }
      // Use soft delete instead of permanent delete here
      deleteProject(new ObjectId(project.getId())); // This now performs soft delete
    }
    logger.info("Bulk deletion successful for project IDs: {}", projectIds);
  }

  @Transactional
  public ProjectDto createProjectFromTemplate(String templateName, String newProjectName, String email)
      throws IOException {
    logger.info("Creating project from template: {} with name: {} for email: {}", templateName, newProjectName, email);

    // 1. Create the project in the database
    Project newProject = Project.builder()
        .projectName(newProjectName)
        .description("Project created from template: " + templateName)
        .build();
    Project createdProject = createProject(newProject, email);

    Path templatePath = Paths.get("src/main/resources/project_templates", templateName);
    Path projectPath = Paths.get(projectBasePath, createdProject.getId().toString());

    // 3. Copy the template contents to the new project directory
    if (!Files.exists(templatePath)) {
      throw new IllegalArgumentException("Template not found: " + templateName);
    }

    try {
      Files.walk(templatePath)
          .forEach(source -> {
            Path destination = projectPath.resolve(templatePath.relativize(source));
            try {
              Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
              logger.error("Error copying file: {}", source, e);
              throw new RuntimeException("Error copying template files", e);
            }
          });
    } catch (IOException e) {
      logger.error("Error copying template directory", e);
      throw new RuntimeException("Error copying template directory", e);
    }
    createdProject.setProjectDirectory(projectPath.toString());
    projectRepository.save(createdProject);

    return projectMapper.toDTO(createdProject);
  }

  // Validation Helpers
  private void validateProject(Project project) {
    if (project == null || project.getProjectName() == null || project.getProjectName().trim().isEmpty()) {
      logger.error("Invalid project: {}", project);
      throw new IllegalArgumentException("Project and project name cannot be null or empty");
    }
  }

  private void validateObjectId(ObjectId id, String fieldName) {
    if (id == null) {
      logger.error("{} cannot be null", fieldName);
      throw new IllegalArgumentException(fieldName + " cannot be null");
    }
  }

  private void validateString(String value, String fieldName) {
    if (value == null || value.trim().isEmpty()) {
      logger.error("{} cannot be null or empty", fieldName);
      throw new IllegalArgumentException(fieldName + " cannot be null or empty");
    }
  }

  /**
   * Gets user email by their ObjectId
   *
   * @param userId The ObjectId of the user
   * @return The user's email address
   */
  public String getUserEmailById(ObjectId userId) {
    return userRepository.findById(userId)
        .map(User::getEmail)
        .orElseThrow(() -> {
          logger.error("User not found with ID: {}", userId);
          return new UsernameNotFoundException("User not found with ID: " + userId);
        });
  }

  private void validatePageable(Pageable pageable) {
    if (pageable == null) {
      logger.error("Pageable cannot be null");
      throw new IllegalArgumentException("Pageable cannot be null");
    }
    if (pageable.getPageNumber() < 0 || pageable.getPageSize() <= 0) {
      logger.error("Invalid pageable parameters: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
      throw new IllegalArgumentException("Page must be non-negative and size must be positive");
    }
  }

  private User getUserByEmail(String email, String errorMessage) {
    validateString(email, "Email");
    return userRepository.findByEmail(email)
        .orElseThrow(() -> {
          logger.error(errorMessage);
          return new UsernameNotFoundException(errorMessage);
        });
  }

  private User validateOwner(Project project, String email, String action) {
    User user = getUserByEmail(email, "User not found: " + email);
    if (!project.getOwner().equals(user)) {
      logger.error("Access denied for email: {} to {} project: {}", email, action, project.getId());
      throw new AccessDeniedException("Only the project owner can " + action + " the project");
    }
    return user;
  }

  /**
   * Retrieves soft-deleted projects for a user with pagination.
   *
   * @param email    The email of the user.
   * @param pageable Pagination information.
   * @return A page of soft-deleted projects as ProjectDTO.
   * @throws UsernameNotFoundException If the user is not found.
   */
  public Page<ProjectDto> getDeletedProjects(String email, Pageable pageable) {
    logger.info("Fetching soft-deleted projects for email: {}", email);
    validatePageable(pageable);
    User user = getUserByEmail(email, "User not found: " + email);
    // Pass the owner's ObjectId instead of the User object
    Page<Project> projects = projectRepository.findByOwnerAndDeletedTrue(new ObjectId(user.getId()), pageable);
    return projectMapper.toDTOPage(projects);
  }
}
