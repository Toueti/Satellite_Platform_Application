package com.enit.satellite_platform.project_management.service;

import com.enit.satellite_platform.exception.DuplicateNameException;

import com.enit.satellite_platform.project_management.exception.ProjectNotFoundException;
import com.enit.satellite_platform.project_management.model.Project;
import com.enit.satellite_platform.project_management.dto.ProjectStatisticsDto;
import com.enit.satellite_platform.project_management.repository.ProjectRepository;
import com.enit.satellite_platform.resources_management.repositories.ImageRepository;
import com.enit.satellite_platform.resources_management.services.ImageService;
import com.enit.satellite_platform.user_management.model.User;
import com.enit.satellite_platform.user_management.repository.UserRepository;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service class for managing projects.
 * Provides methods for creating, retrieving, updating, deleting, sharing, and
 * performing other operations on projects.
 */
@Service
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

  /**
   * Service for managing image-related operations.
   */
  @Autowired
  private ImageService imageService;

  /**
   * Creates a new project.
   *
   * @param project The project to create.
   * @param email   The email of the user creating the project.
   * @return The created project.
   * @throws IllegalArgumentException  If the project data is invalid or a project
   *                                   with the same name already exists for the
   *                                   user.
   * @throws UsernameNotFoundException If the user with the given email is not
   *                                   found.
   * @throws RuntimeException          If an unexpected error occurs during
   *                                   project creation.
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
      thematician.getProjects().add(savedProject);
      userRepository.save(thematician);
      logger.info("Project created successfully with ID: {}", savedProject.getProjectID());
      return savedProject;
    } catch (DataIntegrityViolationException e) {
      logger.error("Duplicate project name for user: {}", email, e);
      throw new DuplicateNameException("A project with the same name already exists for this user.");
    } catch (Exception e) {
      logger.error("Failed to create project", e);
      throw new RuntimeException("Failed to create project: " + e.getMessage(), e);
    }
  }

  @Transactional
  public Project renameProject(ObjectId projectId, String newName, String email) {
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
    Optional<Project> existingProject = projectRepository.findByProjectNameAndUserId(owner.getId(), newName);
    if (existingProject.isPresent() && !existingProject.get().getProjectID().equals(projectId)) {
      logger.warn("Project name '{}' already exists for user '{}'", newName, email);
      throw new DuplicateNameException("A project with the name '" + newName + "' already exists for this user.");
    }

    try {
      Project updatedProject = projectRepository.save(project);
      logger.info("Project renamed successfully to: {}", newName);
      return updatedProject;
    } catch (DataIntegrityViolationException e) {
      logger.warn("Duplicate project name '{}' for user '{}'", newName, email);
      throw new DuplicateNameException("A project with the name '" + newName + "' already exists for this user.");
    }
  }

  /**
   * Retrieves a project by its ID.
   *
   * @param id The ID of the project to retrieve.
   * @return The project with the given ID.
   * @throws ProjectNotFoundException If no project with the given ID is found.
   */
  public Project getProject(ObjectId id) {
    logger.info("Fetching project with ID: {}", id);
    validateObjectId(id, "Project ID");
    Project project = projectRepository.findById(id)
        .orElseThrow(() -> {
          logger.error("Project not found with ID: {}", id);
          return new ProjectNotFoundException("Project not found with ID: " + id);
        });
    project.setLastAccessedTime(new Date());
    projectRepository.save(project);
    return project;
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
    List<Project> allProjects = getAllProjects(email);
    long totalProjects = allProjects.size();
    Map<ObjectId, Long> imagesPerProject = new HashMap<>();
    Map<ObjectId, Date> projectTimeIntervals = new HashMap<>();

    for (Project project : allProjects) {
      long imageCount = imageRepository.countByProject(project);
      imagesPerProject.put(project.getProjectID(), imageCount);
      projectTimeIntervals.put(project.getProjectID(), project.getLastAccessedTime());
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
  public List<Project> getAllProjects(String email) {
    logger.info("Fetching all projects for email: {}", email);
    User user = getUserByEmail(email, "User not found");
    return projectRepository.findByOwner(user);
  }

  /**
   * Updates an existing project.
   *
   * @param projectId The ID of the project to update.
   * @param project   The updated project data.
   * @return The updated project.
   * @throws ProjectNotFoundException If no project with the given ID is found.
   * @throws IllegalArgumentException If the project data is invalid.
   * @throws RuntimeException         If an unexpected error occurs during project
   *                                  update.
   */
  @Transactional
  public Project updateProject(ObjectId projectId, Project project) {
    logger.info("Updating project with ID: {}", projectId);
    validateObjectId(projectId, "Project ID");
    validateProject(project);
    Project existingProject = getProject(projectId);
    existingProject.setProjectName(project.getProjectName());
    existingProject.setDescription(project.getDescription());
    existingProject.setImages(project.getImages());
    existingProject.setUpdatedAt(new Date());
    try {
      Project updatedProject = projectRepository.save(existingProject);
      logger.info("Project updated successfully with ID: {}", projectId);
      return updatedProject;
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
    logger.info("Attempting to delete project with ID: {}", projectId);
    validateObjectId(projectId, "Project ID");
    projectRepository.findById(projectId)
        .orElseThrow(() -> {
          logger.error("Project not found with ID: {}", projectId);
          return new ProjectNotFoundException("Project not found with ID: " + projectId);
        });

    try {
      // Delete all images and their GEE data via ImageService
      imageService.deleteAllImagesByProject(projectId);
      projectRepository.deleteById(projectId);
      logger.info("Project and associated images deleted successfully with ID: {}", projectId);
    } catch (Exception e) {
      logger.error("Failed to delete project with ID: {}", projectId, e);
      throw new RuntimeException("Failed to delete project: " + e.getMessage(), e);
    }
  }

 
  /**
   * Shares a project with another user.
   *
   * @param projectId    The ID of the project.
   * @param otherEmail   The email of the user to share with.
   * @param currentEmail The email of the current user (project owner).
   * @return The updated project after sharing.
   * @throws ProjectNotFoundException    If the project with the given ID is not found.
   * @throws UsernameNotFoundException If the user to share with is not found.
   * @throws AccessDeniedException     If the current user is not the owner of the project.
   * @throws IllegalArgumentException  If the sharing request is invalid.
   */
  @Transactional
  public Project shareProject(String projectId, String otherEmail, String currentEmail) {
    logger.info("Sharing project with ID: {} by email: {}", projectId, currentEmail);
    Project project = getProject(new ObjectId(projectId));
    validateOwner(project, currentEmail, "share");

    User userToShare = getUserByEmail(otherEmail, "User not found with email: " + otherEmail);
    project.shareWith(userToShare);
    projectRepository.save(project);
    userToShare.getSharedProjects().add(project);
    userRepository.save(userToShare);
    logger.info("Project shared successfully with user: {}", otherEmail);
    return project;
  }


  /**
   * Unshares a project with another user.
   *
   * @param projectId    The ID of the project.
   * @param otherEmail   The email of the user to unshare with.
   * @param currentEmail The email of the current user (project owner).
   * @return The updated project after unsharing.
   * @throws ProjectNotFoundException  If the project with the given ID is not
   *                                   found.
   * @throws UsernameNotFoundException If the user to unshare with is not found.
   * @throws AccessDeniedException     If the current user is not the owner of the
   *                                   project.
   * @throws IllegalArgumentException  If the sharing request is invalid.
   */
  @Transactional
  public Project unshareProject(String projectId, String otherEmail, String currentEmail) {
    logger.info("Unsharing project with ID: {} by email: {}", projectId, currentEmail);
    Project project = getProject(new ObjectId(projectId));
    validateOwner(project, currentEmail, "unshare");

    User userToUnshare = getUserByEmail(otherEmail, "User not found with email: " + otherEmail);
    project.unshareWith(userToUnshare);
    projectRepository.save(project);
    userToUnshare.getSharedProjects().remove(project);
    userRepository.save(userToUnshare);
    logger.info("Project unshared successfully with user: {}", otherEmail);
    return project;
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
  public Set<User> getSharedUsers(ObjectId projectId, String currentEmail) {
    logger.info("Fetching shared users for project ID: {} by email: {}", projectId, currentEmail);
    validateObjectId(projectId, "Project ID");
    Project project = getProject(projectId);
    User currentUser = getUserByEmail(currentEmail, "Current user not found");
    if (!project.hasAccess(currentUser)) {
      logger.error("Access denied for email: {} to view shared users of project: {}", currentEmail, projectId);
      throw new AccessDeniedException("Access denied to view shared users");
    }
    return project.getSharedUsers();
  }

  /**
   * Retrieves the projects shared with a specific user.
   *
   * @param email The email of the user.
   * @return A list of projects shared with the user.
   * @throws UsernameNotFoundException If the user with the given email is not
   *                                   found.
   */
  public List<Project> getSharedWithMe(String email) {
    logger.info("Fetching projects shared with email: {}", email);
    User user = getUserByEmail(email, "User not found");
    return projectRepository.findBySharedUsersContaining(user);
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
  public List<Project> getLastAccessedProjects(String email, int n) {
    logger.info("Fetching last accessed projects for email: {}, limit: {}", email, n);
    if (n <= 0) {
      logger.error("Limit must be positive: {}", n);
      throw new IllegalArgumentException("Limit must be positive");
    }
    Pageable pageable = PageRequest.of(0, n);
    List<Project> projects = projectRepository.findByOwner_EmailOrderByLastAccessedTimeDesc(email, pageable);

    if (projects.size() < n) {
      User user = getUserByEmail(email, "User not found");
      List<Project> sharedProjects = projectRepository.findBySharedUsersContainingOrderByLastAccessedTimeDesc(user,
          pageable);
      Set<Project> combinedProjects = new LinkedHashSet<>(projects);
      combinedProjects.addAll(sharedProjects);
      projects = new ArrayList<>(combinedProjects);
    }
    return projects.subList(0, Math.min(projects.size(), n));
  }

  /**
   * Archives a project.
   *
   * @param projectId The ID of the project to archive.
   * @param email     The email of the user performing the action.
   * @return The archived project.
   * @throws ProjectNotFoundException If the project with the given ID is not
   *                                  found.
   * @throws AccessDeniedException    If the user is not the owner of the project.
   */
  @Transactional
  public Project archiveProject(ObjectId projectId, String email) {
    logger.info("Archiving project with ID: {} by email: {}", projectId, email);
    Project project = getProject(projectId);
    validateOwner(project, email, "archive");
    project.setArchived(true);
    project.setArchivedDate(new Date());
    return projectRepository.save(project);
  }

  @Transactional
  public Project unarchiveProject(ObjectId projectId, String email) {
    logger.info("Unarchiving project with ID: {} by email: {}", projectId, email);
    Project project = getProject(projectId);
    validateOwner(project, email, "unarchive");
    if (!project.isArchived()) {
      logger.error("Project is not archived: {}", projectId);
      throw new IllegalStateException("Project is not archived");
    }
    project.setArchived(false);
    project.setArchivedDate(null);
    return projectRepository.save(project);
  }

  public List<Project> getArchivedProjects(String email) {
    logger.info("Fetching archived projects for email: {}", email);
    User user = getUserByEmail(email, "User not found: " + email);
    return projectRepository.findByOwnerAndArchivedTrue(user);
  }

  public List<Project> searchProjects(String email, String query, int page, int size) {
    logger.info("Searching projects for email: {} with query: {}, page: {}, size: {}", email, query, page, size);
    User user = getUserByEmail(email, "User not found: " + email);
    validatePageable(page, size);
    Pageable pageable = PageRequest.of(page, size);
    return projectRepository.findByOwnerAndSearchCriteria(user, query, pageable);
  }

  @Transactional
  public Project addTagToProject(ObjectId projectId, String tag, String email) {
    logger.info("Adding tag: {} to project ID: {} by email: {}", tag, projectId, email);
    validateString(tag, "Tag");
    Project project = getProject(projectId);
    validateOwner(project, email, "add tags");
    project.getTags().add(tag);
    return projectRepository.save(project);
  }

  public List<Project> getProjectsByTag(String email, String tag) {
    logger.info("Fetching projects by tag: {} for email: {}", tag, email);
    validateString(tag, "Tag");
    User user = getUserByEmail(email, "User not found: " + email);
    return projectRepository.findByOwnerAndTagsContaining(user, tag);
  }

  @Transactional
  public Project duplicateProject(ObjectId projectId, String newName, String email) {
    logger.info("Duplicating project ID: {} with new name: {} by email: {}", projectId, newName, email);
    validateString(newName, "New project name");
    Project original = getProject(projectId);
    User user = validateOwner(original, email, "duplicate");
    Project duplicate = new Project();
    duplicate.setProjectName(newName);
    duplicate.setDescription(original.getDescription());
    duplicate.setOwner(user);
    duplicate.setImages(new HashSet<>(original.getImages()));
    duplicate.setSharedUsers(new HashSet<>());
    duplicate.setCreatedAt(new Date());
    duplicate.setUpdatedAt(new Date());
    duplicate.setLastAccessedTime(new Date());
    try {
      return projectRepository.save(duplicate);
    } catch (DuplicateKeyException e) {
      logger.error("Duplicate project name: {}", newName, e);
      throw new IllegalArgumentException("A project with the name '" + newName + "' already exists.");
    }
  }

  @Transactional
  public Project updateProjectStatus(ObjectId projectId, String status, String email) {
    logger.info("Updating status of project ID: {} to: {} by email: {}", projectId, status, email);
    validateString(status, "Status");
    Project project = getProject(projectId);
    validateOwner(project, email, "update status");
    project.setStatus(status);
    return projectRepository.save(project);
  }

  public List<Project> getProjectsByStatus(String email, String status) {
    logger.info("Fetching projects by status: {} for email: {}", status, email);
    validateString(status, "Status");
    User user = getUserByEmail(email, "User not found: " + email);
    return projectRepository.findByOwnerAndStatus(user, status);
  }

  public Map<String, Object> exportProject(ObjectId projectId, String email) {
    logger.info("Exporting project ID: {} for email: {}", projectId, email);
    Project project = getProject(projectId);
    User user = getUserByEmail(email, "User not found: " + email);
    if (!project.hasAccess(user)) {
      logger.error("Access denied for email: {} to export project: {}", email, projectId);
      throw new AccessDeniedException("User does not have access to export this project");
    }
    Map<String, Object> exportData = new HashMap<>();
    exportData.put("projectId", project.getProjectID().toString());
    exportData.put("name", project.getProjectName());
    exportData.put("description", project.getDescription());
    exportData.put("owner", project.getOwner().getEmail());
    exportData.put("sharedUsers", project.getSharedUsers().stream().map(User::getEmail).toList());
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
        logger.error("User {} does not own project: {}", email, project.getProjectID());
        throw new AccessDeniedException("User does not own project: " + project.getProjectID());
      }
      deleteProject(project.getProjectID());
    }
    logger.info("Bulk deletion successful for project IDs: {}", projectIds);
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

  private void validatePageable(int page, int size) {
    if (page < 0 || size <= 0) {
      logger.error("Invalid pageable parameters: page={}, size={}", page, size);
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
      logger.error("Access denied for email: {} to {} project: {}", email, action, project.getProjectID());
      throw new AccessDeniedException("Only the project owner can " + action + " the project");
    }
    return user;
  }
}
