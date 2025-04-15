package com.enit.satellite_platform.modules.project_management.services;

import com.enit.satellite_platform.modules.project_management.exceptions.ProjectNotFoundException;
import com.enit.satellite_platform.modules.project_management.repositories.ProjectRepository;
import com.enit.satellite_platform.modules.resource_management.image_management.repositories.ImageRepository;
import com.enit.satellite_platform.modules.resource_management.image_management.services.ImageService;
import com.enit.satellite_platform.exceptions.DuplicationException;
import com.enit.satellite_platform.modules.project_management.dto.ProjectDTO;
import com.enit.satellite_platform.modules.project_management.dto.ProjectStatisticsDto;
import com.enit.satellite_platform.modules.project_management.entities.PermissionLevel;
import com.enit.satellite_platform.modules.project_management.entities.Project;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.User;
import com.enit.satellite_platform.modules.user_management.normal_user_service.repositories.UserRepository;
import com.enit.satellite_platform.shared.mapper.ProjectMapper;

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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

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
      thematician.getProjects().add(savedProject);
      userRepository.save(thematician);
      logger.info("Project created successfully with ID: {}", savedProject.getId());

      return savedProject ;
    } catch (DataIntegrityViolationException e) {
      logger.error("Duplicate project name for user: {}", email, e);
      throw new DuplicationException("A project with the same name already exists for this user.");
    } catch (Exception e) {
      logger.error("Failed to create project", e);
      throw new RuntimeException("Failed to create project: " + e.getMessage(), e);
    }
  }

  @Transactional
  public ProjectDTO renameProject(ObjectId projectId, String newName, String email) {
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
  public ProjectDTO getProject(ObjectId id) {
    return projectMapper.toDTO(getProjectById(id));
  }

  private Project getProjectById(ObjectId id) {
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
   * Retrieves a project by its name.
   *
   * @param projectName The name of the project to retrieve.
   * @return The project with the given name as ProjectDTO.
   * @throws ProjectNotFoundException If no project with the given name is found.
   */
  public ProjectDTO getProjectByName(String projectName) {
    logger.info("Fetching project with name: {}", projectName);
    Project project = projectRepository.findByProjectName(projectName)
        .orElseThrow(() -> {
          logger.error("Project not found with name: {}", projectName);
          return new ProjectNotFoundException("Project not found with name: " + projectName);
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
  public Page<ProjectDTO> getAllProjects(String email, Pageable pageable) {
    logger.info("Fetching all projects for email: {}", email);
    User owner = getUserByEmail(email, "User not found for fetching projects: " + email);
    Page<Project> projects = projectRepository.findByOwnerId(new ObjectId(owner.getId()), pageable);
    if (projects.isEmpty()) {
      logger.warn("No projects found for email: {}", email);
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
  public ProjectDTO updateProject(ObjectId projectId, Project project) {
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
  public ProjectDTO shareProject(String projectId, String otherEmail, String currentEmail, PermissionLevel permission) {
    logger.info("Sharing project with ID: {} with permission {} by email: {}", projectId, permission, currentEmail);
    Project project = getProjectById(new ObjectId(projectId));
    validateOwner(project, currentEmail, "share");

    User userToShare = getUserByEmail(otherEmail, "User not found with email: " + otherEmail);
    project.shareWith(userToShare, permission);
    projectRepository.save(project);
    userToShare.getSharedProjects().add(project);
    userRepository.save(userToShare);
    logger.info("Project shared successfully with user: {}", otherEmail);

    return projectMapper.toDTO(project);
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
  public ProjectDTO unshareProject(String projectId, String otherEmail, String currentEmail) {
    logger.info("Unsharing project with ID: {} by email: {}", projectId, currentEmail);
    Project project = getProjectById(new ObjectId(projectId));
    validateOwner(project, currentEmail, "unshare");

    User userToUnshare = getUserByEmail(otherEmail, "User not found with email: " + otherEmail);
    project.unshareWith(userToUnshare);
    projectRepository.save(project);
    userToUnshare.getSharedProjects().remove(project);
    userRepository.save(userToUnshare);
    logger.info("Project unshared successfully with user: {}", otherEmail);
    return projectMapper.toDTO(project);
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
    Project project = getProjectById(projectId);
    User currentUser = getUserByEmail(currentEmail, "Current user not found");
    if (!project.hasAccess(currentUser)) {
      logger.error("Access denied for email: {} to view shared users of project: {}", currentEmail, projectId);
      throw new AccessDeniedException("Access denied to view shared users");
    }
    // Fetch User objects based on the ObjectIds in the keyset
    Set<ObjectId> sharedUserIds = project.getSharedUsers().keySet();
    return new HashSet<>(userRepository.findAllById(sharedUserIds));
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
    return projectRepository.findBySharedUsersContainsKey(user);
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
   * @return The archived project as ProjectDTO.
   * @throws ProjectNotFoundException If the project with the given ID is not
   *                                  found.
   * @throws AccessDeniedException    If the user is not the owner of the project.
   */
  @Transactional
  public ProjectDTO archiveProject(ObjectId projectId, String email) {
    logger.info("Archiving project with ID: {} by email: {}", projectId, email);
    Project project = getProjectById(projectId);
    validateOwner(project, email, "archive");
    project.setArchived(true);
    project.setArchivedDate(new Date());
    return projectMapper.toDTO(projectRepository.save(project));
  }

  @Transactional
  public ProjectDTO unarchiveProject(ObjectId projectId, String email) {
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
  public ProjectDTO tagProject(ObjectId projectId, String tag, String email) {
    logger.info("Adding tag: {} to project ID: {} by email: {}", tag, projectId, email);
    validateString(tag, "Tag");
    Project project = getProjectById(projectId);
    validateOwner(project, email, "add tags");
    project.getTags().add(tag);
    return projectMapper.toDTO(projectRepository.save(project));
  }

  public List<Project> getProjectsByTag(String email, String tag) {
    logger.info("Fetching projects by tag: {} for email: {}", tag, email);
    validateString(tag, "Tag");
    User user = getUserByEmail(email, "User not found: " + email);
    return projectRepository.findByOwnerAndTagsContaining(user, tag);
  }

  @Transactional
  public ProjectDTO duplicateProject(ObjectId projectId, String newName, String email) {
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
  public ProjectDTO updateProjectStatus(ObjectId projectId, String status, String email) {
    logger.info("Updating status of project ID: {} to: {} by email: {}", projectId, status, email);
    validateString(status, "Status");
    Project project = getProjectById(projectId);
    validateOwner(project, email, "update status");
    project.setStatus(status);
    return projectMapper.toDTO(projectRepository.save(project));
  }

  public List<Project> getProjectsByStatus(String email, String status) {
    logger.info("Fetching projects by status: {} for email: {}", status, email);
    validateString(status, "Status");
    User user = getUserByEmail(email, "User not found: " + email);
    return projectRepository.findByOwnerAndStatus(user, status);
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
      deleteProject(new ObjectId(project.getId()));
    }
    logger.info("Bulk deletion successful for project IDs: {}", projectIds);
  }

  @Transactional
  public ProjectDTO createProjectFromTemplate(String templateName, String newProjectName, String email)
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
      logger.error("Access denied for email: {} to {} project: {}", email, action, project.getId());
      throw new AccessDeniedException("Only the project owner can " + action + " the project");
    }
    return user;
  }
}
