package com.enit.satellite_platform.modules.resource_management.image_management.services;

// Keep only one set of imports
import com.enit.satellite_platform.exceptions.DuplicationException;
import com.enit.satellite_platform.exceptions.ResourceNotFoundException; // Import the new exception
import com.enit.satellite_platform.modules.project_management.exceptions.ProjectNotFoundException;
import com.enit.satellite_platform.modules.project_management.model.PermissionLevel; // Import PermissionLevel
import com.enit.satellite_platform.modules.project_management.model.Project;
import com.enit.satellite_platform.modules.project_management.repositories.ProjectRepository;
import com.enit.satellite_platform.modules.resource_management.image_management.dto.ImageDTO;
import com.enit.satellite_platform.modules.resource_management.image_management.mapper.ImageMapper;
import com.enit.satellite_platform.modules.resource_management.image_management.models.Image;
import com.enit.satellite_platform.modules.resource_management.image_management.repositories.ImageRepository;
import com.enit.satellite_platform.modules.resource_management.image_management.repositories.ImageRepository.ImageMetadataProjection; // Import projection
import com.enit.satellite_platform.modules.resource_management.image_management.repositories.ResultsRepository;
import com.enit.satellite_platform.modules.user_management.models.User; // Import User
import com.enit.satellite_platform.modules.user_management.user_service.repositories.UserRepository; // Import UserRepository

import org.bson.types.ObjectId;
// Keep only one set of imports
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException; // Import AccessDeniedException
import org.springframework.security.core.userdetails.UsernameNotFoundException; // Import UsernameNotFoundException
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class for managing images.
 * This class provides methods to add, delete, retrieve, and count images.
 * It interacts with the {@link ImageRepository}, {@link ProjectRepository}, and
 * {@link ResultsRepository}.
 */
@Service
public class ImageService {
    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ResultsRepository geeResultsRepository;

    @Autowired
    private UserRepository userRepository; // Inject UserRepository

    @Autowired
    private ImageMapper imageMapper;

    /**
     * Adds a new image to the system.
     *
     * @param imageDTO the DTO containing the image details
     * @return the DTO of the added image
     * @throws ProjectNotFoundException if the project associated with the image
     *                                  does not exist
     * @throws IllegalArgumentException if the image DTO is invalid
     * @throws RuntimeException         for any other unexpected errors
     */
    @Transactional
    public ImageDTO addImage(ImageDTO imageDTO) {
        logger.info("Attempting to add image: {}", imageDTO);
        validateImageDTO(imageDTO);
        ObjectId projectId = new ObjectId(imageDTO.getProjectId());
        if (imageRepository.existsByImageIdAndProject_ProjectId(imageDTO.getImageId(), projectId)) {
            logger.warn("Image with Id {} already exists in project {}", imageDTO.getImageId(), projectId);
            throw new DuplicationException(
                    "An image with the id '" + imageDTO.getImageId() + "' already exists in this project.");
        }
        Image image = imageMapper.toEntity(imageDTO);
        try {
            Project project = getProjectById(projectId);
            image.setProject(project);
            image.setRequestTime(new Date());
            image.setUpdatedAt(new Date());
            // Set the file size before saving
            image.setFileSize(image.getImageData() != null ? image.getImageData().length : 0);
            logger.debug("Setting file size for image {} to {} bytes", image.getImageName(), image.getFileSize());
            image = imageRepository.save(image);

            project.getImages().add(image);
            projectRepository.save(project);

            logger.info("Image added successfully with Id: {}", image.getImageId());
            // Return DTO based on the saved full entity
            return imageMapper.toDTO(image);
        } catch (ProjectNotFoundException e) {
            logger.error("Failed to add image: Project not found", e);
            throw e;
        } catch (DataIntegrityViolationException e) {
            logger.warn("Duplicate image name '{}' in project '{}'", imageDTO.getImageName(), projectId);
            throw new DuplicationException(
                    "An image with the name '" + imageDTO.getImageName() + "' already exists in this project.");
        } catch (Exception e) {
            logger.error("Unexpected error while adding image", e);
            throw new RuntimeException("Failed to add image: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Image renameImage(String imageId, String newName, ObjectId projectId) {
        logger.info("Renaming image with Id: {} to new name: {} in project: {}", imageId, newName, projectId);

        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> {
                    logger.error("Image not found with Id: {}", imageId);
                    return new IllegalArgumentException("Image not found with Id: " + imageId);
                });

        // Check if the image belongs to the project
        if (!image.getProject().getProjectId().equals(projectId)) {
            logger.warn("Image {} does not belong to project {}", imageId, projectId);
            throw new IllegalArgumentException("This image does not belong to the specified project.");
        }

        // Check for duplicate name within the same project (excluding this image)
        Optional<Image> existingImage = imageRepository.findByNameAndProjectId(newName, projectId);
        if (existingImage.isPresent() && !existingImage.get().getImageId().equals(imageId)) {
            logger.warn("Image name '{}' already exists in project '{}'", newName, projectId);
            throw new DuplicationException(
                    "An image with the name '" + newName + "' already exists in this project.");
        }

        image.setImageName(newName);
        image.setUpdatedAt(new Date()); // Update timestamp on rename
        Image updatedImage = imageRepository.save(image);
        logger.info("Image renamed successfully to: {}", newName);
        return updatedImage; // Returning entity, consider returning DTO if needed by controller
    }

    /**
     * Deletes an image by its Id.
     *
     * @param id the Id of the image to delete
     * @throws IllegalArgumentException if the image Id is invalid
     * @throws RuntimeException         for any other unexpected errors
     */
    @Transactional
    public void deleteImage(String id) {
        logger.info("Attempting to delete image with Id: {}", id);
        validateImageId(id);

        Image image = imageRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Image not found with Id: {}", id);
                    return new IllegalArgumentException("Image not found with Id: " + id);
                });

        try {
            // Delete associated GeeResults
            geeResultsRepository.deleteAllByImage_ImageId(id);
            logger.info("Deleted GeeResults associated with image Id: {}", id);

            // Remove image from the project's image set
            Project project = image.getProject();
            if (project != null) {
                project.getImages().removeIf(img -> img.getImageId().equals(id));
                projectRepository.save(project);
                logger.info("Removed image Id: {} from project Id: {}", id, project.getProjectId());
            }

            imageRepository.deleteById(id);
            logger.info("Image deleted successfully with Id: {}", id);
        } catch (Exception e) {
            logger.error("Failed to delete image with Id: {}", id, e);
            throw new RuntimeException("Failed to delete image: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves all images with pagination.
     *
     * @param pageable the pagination information
     * @return a page of image DTOs
     * @throws IllegalArgumentException if the pageable is invalid
     * @throws RuntimeException         for any other unexpected errors
     */
    public Page<ImageDTO> getAllImages(Pageable pageable) {
        logger.info("Retrieving all images with pageable: {}", pageable);
        validatePageable(pageable);

        try {
            // Use projection method
            Page<ImageMetadataProjection> page = imageRepository.findAllProjectedBy(pageable);
            // Map projection page to DTO page
            List<ImageDTO> dtoList = imageMapper.projectionToDTOList(page.getContent());
            return new PageImpl<>(dtoList, pageable, page.getTotalElements());
        } catch (Exception e) {
            logger.error("Failed to retrieve images", e);
            throw new RuntimeException("Failed to retrieve images: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves an image by its name and project Id.
     *
     * @param name      the name of the image
     * @param projectId the Id of the project
     * @return an optional containing the image DTO if found, otherwise empty
     * @throws IllegalArgumentException if the image name or project Id is invalid
     * @throws RuntimeException         for any other unexpected errors
     */
    public Optional<ImageDTO> getImageByName(String name, ObjectId projectId) {
        logger.info("Retrieving image by name: {} and projectId: {}", name, projectId);
        validateString(name, "Image name");
        validateObjectId(projectId, "Project Id");

        try {
            return imageRepository.findByImageNameAndProject_ProjectId(name, projectId)
                    .map(imageMapper::toDTO);
        } catch (Exception e) {
            logger.error("Failed to retrieve image by name and project", e);
            throw new RuntimeException("Failed to retrieve image: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves an image by its Id.
     *
     * @param id the Id of the image
     * @return the image DTO
     * @throws IllegalArgumentException if the image Id is invalid
     * @throws RuntimeException         for any other unexpected errors
     */
    public ImageDTO getImageById(String id) {
        logger.info("Retrieving image by Id: {}", id);
        validateImageId(id);

        try {
            // Use projection method
            return imageRepository.findProjectedByImageId(id)
                    .map(imageMapper::toDTO) // Map projection to DTO
                    // Correct orElseThrow syntax
                    .orElseThrow(() -> new ResourceNotFoundException("Image metadata not found with Id: " + id));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to retrieve image metadata by Id: {}", id, e);
            throw new RuntimeException("Failed to retrieve image metadata: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the raw image data (byte array) for a given image ID.
     *
     * @param id The ID of the image.
     * @return The byte array of the image data.
     * @throws ResourceNotFoundException if the image is not found.
     * @throws RuntimeException          for any other unexpected errors.
     */
    public byte[] getImageData(String id) {
        logger.info("Retrieving image data for Id: {}", id);
        validateImageId(id);
        try {
            Image image = imageRepository.findById(id)
                    // Correct orElseThrow syntax
                    .orElseThrow(() -> new ResourceNotFoundException("Image not found with Id: " + id));
            if (image.getImageData() == null) {
                 logger.warn("Image data is null for Id: {}", id);
                 // Depending on requirements, could return empty array or throw exception
                 return new byte[0];
            }
            logger.debug("Returning image data for Id: {} (Size: {} bytes)", id, image.getImageData().length);
            return image.getImageData();
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to retrieve image data for Id: {}", id, e);
            throw new RuntimeException("Failed to retrieve image data: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves all images by project Id.
     *
     * @param projectId the Id of the project
     * @return a list of image DTOs
     * @throws IllegalArgumentException if the project Id is invalid
     * @throws RuntimeException         for any other unexpected errors
     */
    public List<ImageDTO> getImagesByProject(ObjectId projectId) {
        logger.info("Retrieving images by project Id: {}", projectId);
        validateObjectId(projectId, "Project Id");

        try {
            getProjectById(projectId); // Validate project exists
            // Use projection method
            List<ImageMetadataProjection> projections = imageRepository.findAllByProject_ProjectIdProjectedBy(projectId);
            return imageMapper.projectionToDTOList(projections); // Map projections to DTOs
        } catch (Exception e) {
            logger.error("Failed to retrieve images by project Id: {}", projectId, e);
            throw new RuntimeException("Failed to retrieve images: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes all images for a given project.
     *
     * @param projectId the Id of the project
     * @throws IllegalArgumentException if the project Id is invalid
     * @throws ProjectNotFoundException if the project does not exist
     * @throws RuntimeException         for any other unexpected errors
     */
    @Transactional
    public void deleteAllImagesByProject(ObjectId projectId) {
        logger.info("Deleting all images for project Id: {}", projectId);
        validateObjectId(projectId, "Project Id");

        try {
            Project project = getProjectById(projectId);
            List<Image> images = imageRepository.findAllByProject_ProjectId(projectId);
            for (Image image : images) {
                geeResultsRepository.deleteAllByImage_ImageId(image.getImageId());
                logger.info("Deleted GeeResults for image Id: {}", image.getImageId());
            }
            imageRepository.deleteAllByProject_ProjectId(projectId);
            project.getImages().clear();
            projectRepository.save(project);
            logger.info("All images and GEE results deleted successfully for project Id: {}", projectId);
        } catch (ProjectNotFoundException e) {
            logger.error("Project not found for deleting images: {}", projectId, e);
            throw e;
        } catch (Exception e) {
            logger.error("Failed to delete images for project Id: {}", projectId, e);
            throw new RuntimeException("Failed to delete images: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves an image by its image Id and project Id.
     *
     * @param imageId   the Id of the image
     * @param projectId the Id of the project
     * @return an optional containing the image DTO if found, otherwise empty
     * @throws IllegalArgumentException if the image Id or project Id is invalid
     * @throws RuntimeException         for any other unexpected errors
     */
    public Optional<ImageDTO> getImageByImageIdAndProject(String imageId, ObjectId projectId) {
        logger.info("Retrieving image by image Id: {} and project Id: {}", imageId, projectId);
        validateImageId(imageId);
        validateObjectId(projectId, "Project Id");

        try {
            return imageRepository.findByImageIdAndProject_ProjectId(imageId, projectId)
                    .map(imageMapper::toDTO);
        } catch (Exception e) {
            logger.error("Failed to retrieve image by image Id and project Id", e);
            throw new RuntimeException("Failed to retrieve image: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes an image by its image Id and project Id.
     *
     * @param imageId   the Id of the image
     * @param projectId the Id of the project
     * @throws IllegalArgumentException if the image Id or project Id is invalid
     * @throws RuntimeException         for any other unexpected errors
     */
    @Transactional
    public void deleteImageByProject(String imageId, ObjectId projectId) {
        logger.info("Deleting image by image Id: {} and project Id: {}", imageId, projectId);
        validateImageId(imageId);
        validateObjectId(projectId, "Project Id");

        try {
            imageRepository.findByImageIdAndProject_ProjectId(imageId, projectId)
                    .orElseThrow(() -> {
                        logger.error("Image not found with Id: {} in project: {}", imageId, projectId);
                        return new IllegalArgumentException(
                                "Image not found with Id: " + imageId + " in project: " + projectId);
                    });
            geeResultsRepository.deleteAllByImage_ImageId(imageId);
            Project project = getProjectById(projectId);
            project.getImages().removeIf(img -> img.getImageId().equals(imageId));
            projectRepository.save(project);
            imageRepository.deleteByImageIdAndProject_ProjectId(imageId, projectId);
            logger.info("Image and GEE results deleted successfully with Id: {} from project: {}", imageId, projectId);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to delete image by image Id: {} and project Id: {}", imageId, projectId, e);
            throw new RuntimeException("Failed to delete image: " + e.getMessage(), e);
        }
    }

    /**
     * Bulk deletes multiple images by their Ids.
     *
     * @param imageIds the list of image Ids to delete
     * @throws IllegalArgumentException if the image Ids list is invalid or contains
     *                                  non-existent Ids
     * @throws RuntimeException         for any other unexpected errors
     */
    @Transactional
    public void bulkDeleteImages(List<String> imageIds) {
        logger.info("Attempting to bulk delete images with Ids: {}", imageIds);
        validateImageIds(imageIds);

        try {
            List<String> invalidIds = imageIds.stream()
                    .filter(id -> !imageRepository.existsById(id))
                    .collect(Collectors.toList());
            if (!invalidIds.isEmpty()) {
                logger.error("Images not found with Ids: {}", invalidIds);
                throw new IllegalArgumentException("Images not found with Ids: " + invalidIds);
            }
            for (String id : imageIds) {
                deleteImage(id); // Reuse deleteImage for cascading GEE deletion
            }
            logger.info("Bulk deletion successful for image Ids: {}", imageIds);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to bulk delete images", e);
            throw new RuntimeException("Failed to bulk delete images: " + e.getMessage(), e);
        }
    }

    /**
     * Counts the number of images for a given project.
     *
     * @param projectId the Id of the project
     * @return the count of images
     * @throws IllegalArgumentException if the project Id is invalid
     * @throws ProjectNotFoundException if the project does not exist
     * @throws RuntimeException         for any other unexpected errors
     */
    public long countImagesByProject(ObjectId projectId) {
        logger.info("Counting images for project Id: {}", projectId);
        validateObjectId(projectId, "Project Id");

        try {
            getProjectById(projectId);
            return imageRepository.countByProject_ProjectId(projectId);
        } catch (ProjectNotFoundException e) {
            logger.error("Project not found for counting images: {}", projectId, e);
            throw e;
        } catch (Exception e) {
            logger.error("Failed to count images for project Id: {}", projectId, e);
            throw new RuntimeException("Failed to count images: " + e.getMessage(), e);
        }
    }


    /**
     * Imports images from a source project to a target project.
     *
     * @param sourceProjectId The ID of the source project.
     * @param targetProjectId The ID of the target project.
     * @param imageIds        The list of image IDs to import.
     * @param userEmail       The email of the user performing the import.
     * @return A list of DTOs for the newly imported images.
     * @throws ProjectNotFoundException  if source or target project not found.
     * @throws ResourceNotFoundException if any source image ID is not found.
     * @throws AccessDeniedException     if the user lacks necessary permissions.
     * @throws UsernameNotFoundException if the user performing the action is not found.
     * @throws RuntimeException          for other unexpected errors.
     */
    @Transactional
    public List<ImageDTO> importImagesFromProject(String sourceProjectId, String targetProjectId, List<String> imageIds, String userEmail) {
        logger.info("Importing images {} from project {} to project {} by user {}", imageIds, sourceProjectId, targetProjectId, userEmail);

        ObjectId sourceProjId = parseObjectId(sourceProjectId, "Source Project ID");
        ObjectId targetProjId = parseObjectId(targetProjectId, "Target Project ID");
        validateImageIds(imageIds);
        validateString(userEmail, "User Email");

        if (sourceProjId.equals(targetProjId)) {
            throw new IllegalArgumentException("Source and target project cannot be the same.");
        }

        User user = getUserByEmail(userEmail, "User performing import not found");
        Project sourceProject = getProjectById(sourceProjId);
        Project targetProject = getProjectById(targetProjId);

        // --- Permission Checks ---
        // Assuming Project model has methods like hasReadAccess/hasWriteAccess
        // Adjust these checks based on the actual implementation in Project.java
        if (!sourceProject.hasAccess(user, PermissionLevel.READ)) { // Check for READ access on source
             logger.warn("User {} lacks READ access to source project {}", userEmail, sourceProjectId);
             throw new AccessDeniedException("User does not have read access to the source project.");
        }
        if (!targetProject.hasAccess(user, PermissionLevel.WRITE)) { // Check for WRITE access on target
             logger.warn("User {} lacks WRITE access to target project {}", userEmail, targetProjectId);
             throw new AccessDeniedException("User does not have write access to the target project.");
        }
        // --- End Permission Checks ---


        List<Image> sourceImages = imageRepository.findAllById(imageIds);
        if (sourceImages.size() != imageIds.size()) {
            List<String> foundIds = sourceImages.stream().map(Image::getImageId).collect(Collectors.toList());
            List<String> missingIds = new ArrayList<>(imageIds); // Create a mutable list of requested IDs
            missingIds.removeAll(foundIds); // Remove the IDs that were found
            logger.error("Some source images not found: {}", missingIds);
            throw new ResourceNotFoundException("Could not find source images with IDs: " + missingIds);
        }

        List<Image> importedImages = new ArrayList<>();
        for (Image sourceImage : sourceImages) {
            // Verify the image actually belongs to the source project (paranoid check)
            if (!sourceImage.getProject().getProjectId().equals(sourceProjId)) {
                 logger.error("Image {} does not belong to source project {}", sourceImage.getImageId(), sourceProjId);
                 // Handle this inconsistency - skip or throw error
                 continue; // Skipping for now
            }

            Image newImage = new Image();
            String originalName = sourceImage.getImageName();
            String newName = findAvailableName(originalName, targetProjId);

            newImage.setImageName(newName);
            newImage.setImageData(sourceImage.getImageData()); // Copy image data
            newImage.setFileSize(sourceImage.getFileSize());
            newImage.setMettadata(sourceImage.getMettadata() != null ? new java.util.HashMap<>(sourceImage.getMettadata()) : null); // Deep copy metadata map
            newImage.setProject(targetProject); // Set target project
            newImage.setRequestTime(new Date()); // Set new timestamps
            newImage.setUpdatedAt(new Date());
            // Results are not copied

            try {
                Image savedImage = imageRepository.save(newImage);
                targetProject.getImages().add(savedImage); // Add to target project's list
                importedImages.add(savedImage);
                logger.info("Successfully imported image {} as {} to project {}", sourceImage.getImageId(), savedImage.getImageName(), targetProjId);
            } catch (DataIntegrityViolationException e) {
                 // This might happen if findAvailableName logic fails or due to race conditions
                 logger.error("Data integrity violation while saving imported image '{}' to project {}", newName, targetProjId, e);
                 // Decide how to handle: skip this image, retry with different name, or throw
                 throw new RuntimeException("Failed to save imported image due to potential name conflict: " + newName, e);
            }
        }

        projectRepository.save(targetProject); // Save changes to target project's image list
        logger.info("Finished importing {} images to project {}", importedImages.size(), targetProjId);

        return imageMapper.toDTOList(importedImages);
    }

    /**
     * Finds an available name in the target project, appending _copy_n if necessary.
     */
    private String findAvailableName(String originalName, ObjectId targetProjectId) {
        String currentName = originalName;
        int copyCount = 0;
        // Check if the original name exists
        while (imageRepository.existsByNameAndProjectId(currentName, targetProjectId)) {
            copyCount++;
            currentName = originalName + "_copy_" + copyCount;
            // Optional: Add a limit to prevent infinite loops in edge cases
            if (copyCount > 100) { // Example limit
                 logger.error("Could not find available name for '{}' in project {} after {} attempts", originalName, targetProjectId, copyCount);
                 throw new RuntimeException("Failed to find an available name for import: " + originalName);
            }
        }
        if (copyCount > 0) {
            logger.debug("Name conflict for '{}', using '{}' instead in project {}", originalName, currentName, targetProjectId);
        }
        return currentName;
    }


    // Validation Helpers

    /**
     * Validates the image DTO.
     *
     * @param imageDTO the image DTO to validate
     * @throws IllegalArgumentException if the image DTO is invalid
     */
    private void validateImageDTO(ImageDTO imageDTO) {
        if (imageDTO == null || imageDTO.getImageName() == null || imageDTO.getImageName().trim().isEmpty()) {
            logger.error("Invalid ImageDTO: {}", imageDTO);
            throw new IllegalArgumentException("ImageDTO and image name cannot be null or empty");
        }
        try {
            new ObjectId(imageDTO.getProjectId());
        } catch (Exception e) {
            logger.error("Invalid project Id in ImageDTO: {}", imageDTO.getProjectId());
            throw new IllegalArgumentException("Invalid project Id: " + imageDTO.getProjectId());
        }
        // Removed imageData validation as it's handled by MultipartFile
    }

    /**
     * Parses a string into an ObjectId.
     *
     * @param idString  The string to parse.
     * @param fieldName The name of the field for error messages.
     * @return The ObjectId.
     * @throws IllegalArgumentException if the string is not a valid ObjectId.
     */
    private ObjectId parseObjectId(String idString, String fieldName) {
        validateString(idString, fieldName);
        try {
            return new ObjectId(idString);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid {} format: {}", fieldName, idString);
            throw new IllegalArgumentException("Invalid " + fieldName + " format: " + idString);
        }
    }

    /**
     * Validates the image Id.
     *
     * @param id the image Id to validate
     * @throws IllegalArgumentException if the image Id is invalid
     */
    private void validateImageId(String id) {
        validateString(id, "Image Id");
    }

    /**
     * Validates the list of image Ids.
     *
     * @param imageIds the list of image Ids to validate
     * @throws IllegalArgumentException if the list of image Ids is invalid
     */
    private void validateImageIds(List<String> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            logger.error("Image Ids list is null or empty");
            throw new IllegalArgumentException("Image Ids list cannot be null or empty");
        }
    }

    /**
     * Validates a string value.
     *
     * @param value     the string value to validate
     * @param fieldName the name of the field being validated
     * @throws IllegalArgumentException if the string value is invalid
     */
    private void validateString(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            logger.error("{} cannot be null or empty", fieldName);
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
    }

    /**
     * Validates an ObjectId.
     *
     * @param id        the ObjectId to validate
     * @param fieldName the name of the field being validated
     * @throws IllegalArgumentException if the ObjectId is invalid
     */
    private void validateObjectId(ObjectId id, String fieldName) {
        if (id == null) {
            logger.error("{} cannot be null", fieldName);
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
    }

    /**
     * Retrieves a user by email.
     *
     * @param email        The email of the user.
     * @param errorMessage The error message if the user is not found.
     * @return The User entity.
     * @throws UsernameNotFoundException if the user is not found.
     */
    private User getUserByEmail(String email, String errorMessage) {
        validateString(email, "Email");
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error(errorMessage + ": {}", email);
                    return new UsernameNotFoundException(errorMessage + ": " + email);
                });
    }

    /**
     * Validates the pageable object.
     *
     * @param pageable the pageable object to validate
     * @throws IllegalArgumentException if the pageable object is invalid
     */
    private void validatePageable(Pageable pageable) {
        if (pageable == null || pageable.getPageNumber() < 0 || pageable.getPageSize() <= 0) {
            logger.error("Invalid pageable: {}", pageable);
            throw new IllegalArgumentException("Pageable must be valid with non-negative page and positive size");
        }
    }

    /**
     * Retrieves a project by its Id.
     *
     * @param projectId the Id of the project
     * @return the project
     * @throws ProjectNotFoundException if the project does not exist
     */
    private Project getProjectById(ObjectId projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> {
                    logger.error("Project not found with Id: {}", projectId);
                    return new ProjectNotFoundException("Project not found with Id: " + projectId);
                });
    }
}
