package com.enit.satellite_platform.resources_management.services;

import com.enit.satellite_platform.exception.DuplicateNameException;
import com.enit.satellite_platform.project_management.exception.ProjectNotFoundException;
import com.enit.satellite_platform.project_management.model.Project;
import com.enit.satellite_platform.project_management.repository.ProjectRepository;
import com.enit.satellite_platform.resources_management.dto.ImageDTO;
import com.enit.satellite_platform.resources_management.mapper.ImageMapper;
import com.enit.satellite_platform.resources_management.models.Image;
import com.enit.satellite_platform.resources_management.repositories.GeeResultsRepository;
import com.enit.satellite_platform.resources_management.repositories.ImageRepository;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class for managing images.
 * This class provides methods to add, delete, retrieve, and count images.
 * It interacts with the {@link ImageRepository}, {@link ProjectRepository}, and
 * {@link GeeResultsRepository}.
 */
@Service
public class ImageService {
    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private GeeResultsRepository geeResultsRepository;

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
        if (imageRepository.existsByImageIdAndProject_ProjectID(imageDTO.getImageId(), projectId)) {
            logger.warn("Image with ID {} already exists in project {}", imageDTO.getImageId(), projectId);
            throw new DuplicateNameException(
                    "An image with the id '" + imageDTO.getImageId() + "' already exists in this project.");
        }
        Image image = imageMapper.toEntity(imageDTO);
        try {
            Project project = getProjectById(projectId);
            image.setProject(project);
            image.setRequestTime(new Date());
            image.setUpdatedAt(new Date());
            image = imageRepository.save(image);

            project.getImages().add(image);
            projectRepository.save(project);

            logger.info("Image added successfully with ID: {}", image.getImageId());
            return imageMapper.toDTO(image);
        } catch (ProjectNotFoundException e) {
            logger.error("Failed to add image: Project not found", e);
            throw e;
        } catch (DataIntegrityViolationException e) {
            logger.warn("Duplicate image name '{}' in project '{}'", imageDTO.getImageName(), projectId);
            throw new DuplicateNameException(
                    "An image with the name '" + imageDTO.getImageName() + "' already exists in this project.");
        } catch (Exception e) {
            logger.error("Unexpected error while adding image", e);
            throw new RuntimeException("Failed to add image: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Image renameImage(String imageId, String newName, ObjectId projectId) {
        logger.info("Renaming image with ID: {} to new name: {} in project: {}", imageId, newName, projectId);

        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> {
                    logger.error("Image not found with ID: {}", imageId);
                    return new IllegalArgumentException("Image not found with ID: " + imageId);
                });

        // Check if the image belongs to the project
        if (!image.getProject().getProjectID().equals(projectId)) {
            logger.warn("Image {} does not belong to project {}", imageId, projectId);
            throw new IllegalArgumentException("This image does not belong to the specified project.");
        }

        // Check for duplicate name within the same project (excluding this image)
        Optional<Image> existingImage = imageRepository.findByNameAndProjectId(newName, projectId);
        if (existingImage.isPresent() && !existingImage.get().getImageId().equals(imageId)) {
            logger.warn("Image name '{}' already exists in project '{}'", newName, projectId);
            throw new DuplicateNameException(
                    "An image with the name '" + newName + "' already exists in this project.");
        }

        image.setImageName(newName);
        Image updatedImage = imageRepository.save(image);
        logger.info("Image renamed successfully to: {}", newName);
        return updatedImage;
    }

    /**
     * Deletes an image by its ID.
     *
     * @param id the ID of the image to delete
     * @throws IllegalArgumentException if the image ID is invalid
     * @throws RuntimeException         for any other unexpected errors
     */
    @Transactional
    public void deleteImage(String id) {
        logger.info("Attempting to delete image with ID: {}", id);
        validateImageId(id);

        Image image = imageRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Image not found with ID: {}", id);
                    return new IllegalArgumentException("Image not found with ID: " + id);
                });

        try {
            // Delete associated GeeResults
            geeResultsRepository.deleteAllByImage_ImageId(id);
            logger.info("Deleted GeeResults associated with image ID: {}", id);

            // Remove image from the project's image set
            Project project = image.getProject();
            if (project != null) {
                project.getImages().removeIf(img -> img.getImageId().equals(id));
                projectRepository.save(project);
                logger.info("Removed image ID: {} from project ID: {}", id, project.getProjectID());
            }

            imageRepository.deleteById(id);
            logger.info("Image deleted successfully with ID: {}", id);
        } catch (Exception e) {
            logger.error("Failed to delete image with ID: {}", id, e);
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
            return imageRepository.findAll(pageable).map(imageMapper::toDTO);
        } catch (Exception e) {
            logger.error("Failed to retrieve images", e);
            throw new RuntimeException("Failed to retrieve images: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves an image by its name and project ID.
     *
     * @param name      the name of the image
     * @param projectId the ID of the project
     * @return an optional containing the image DTO if found, otherwise empty
     * @throws IllegalArgumentException if the image name or project ID is invalid
     * @throws RuntimeException         for any other unexpected errors
     */
    public Optional<ImageDTO> getImageByName(String name, ObjectId projectId) {
        logger.info("Retrieving image by name: {} and projectId: {}", name, projectId);
        validateString(name, "Image name");
        validateObjectId(projectId, "Project ID");

        try {
            return imageRepository.findByImageNameAndProject_ProjectID(name, projectId)
                    .map(imageMapper::toDTO);
        } catch (Exception e) {
            logger.error("Failed to retrieve image by name and project", e);
            throw new RuntimeException("Failed to retrieve image: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves an image by its ID.
     *
     * @param id the ID of the image
     * @return the image DTO
     * @throws IllegalArgumentException if the image ID is invalid
     * @throws RuntimeException         for any other unexpected errors
     */
    public ImageDTO getImageById(String id) {
        logger.info("Retrieving image by ID: {}", id);
        validateImageId(id);

        try {
            return imageRepository.findById(id)
                    .map(imageMapper::toDTO)
                    .orElseThrow(() -> {
                        logger.error("Image not found with ID: {}", id);
                        return new IllegalArgumentException("Image not found with ID: " + id);
                    });
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to retrieve image by ID: {}", id, e);
            throw new RuntimeException("Failed to retrieve image: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves all images by project ID.
     *
     * @param projectId the ID of the project
     * @return a list of image DTOs
     * @throws IllegalArgumentException if the project ID is invalid
     * @throws RuntimeException         for any other unexpected errors
     */
    public List<ImageDTO> getImagesByProject(ObjectId projectId) {
        logger.info("Retrieving images by project ID: {}", projectId);
        validateObjectId(projectId, "Project ID");

        try {
            getProjectById(projectId); // Validate project exists
            List<Image> images = imageRepository.findAllByProject_ProjectID(projectId);
            return imageMapper.toDTOList(images);
        } catch (Exception e) {
            logger.error("Failed to retrieve images by project ID: {}", projectId, e);
            throw new RuntimeException("Failed to retrieve images: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes all images for a given project.
     *
     * @param projectId the ID of the project
     * @throws IllegalArgumentException if the project ID is invalid
     * @throws ProjectNotFoundException if the project does not exist
     * @throws RuntimeException         for any other unexpected errors
     */
    @Transactional
    public void deleteAllImagesByProject(ObjectId projectId) {
        logger.info("Deleting all images for project ID: {}", projectId);
        validateObjectId(projectId, "Project ID");

        try {
            Project project = getProjectById(projectId);
            List<Image> images = imageRepository.findAllByProject_ProjectID(projectId);
            for (Image image : images) {
                geeResultsRepository.deleteAllByImage_ImageId(image.getImageId());
                logger.info("Deleted GeeResults for image ID: {}", image.getImageId());
            }
            imageRepository.deleteAllByProject_ProjectID(projectId);
            project.getImages().clear();
            projectRepository.save(project);
            logger.info("All images and GEE results deleted successfully for project ID: {}", projectId);
        } catch (ProjectNotFoundException e) {
            logger.error("Project not found for deleting images: {}", projectId, e);
            throw e;
        } catch (Exception e) {
            logger.error("Failed to delete images for project ID: {}", projectId, e);
            throw new RuntimeException("Failed to delete images: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves an image by its image ID and project ID.
     *
     * @param imageId   the ID of the image
     * @param projectId the ID of the project
     * @return an optional containing the image DTO if found, otherwise empty
     * @throws IllegalArgumentException if the image ID or project ID is invalid
     * @throws RuntimeException         for any other unexpected errors
     */
    public Optional<ImageDTO> getImageByImageIdAndProject(String imageId, ObjectId projectId) {
        logger.info("Retrieving image by image ID: {} and project ID: {}", imageId, projectId);
        validateImageId(imageId);
        validateObjectId(projectId, "Project ID");

        try {
            return imageRepository.findByImageIdAndProject_ProjectID(imageId, projectId)
                    .map(imageMapper::toDTO);
        } catch (Exception e) {
            logger.error("Failed to retrieve image by image ID and project ID", e);
            throw new RuntimeException("Failed to retrieve image: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes an image by its image ID and project ID.
     *
     * @param imageId   the ID of the image
     * @param projectId the ID of the project
     * @throws IllegalArgumentException if the image ID or project ID is invalid
     * @throws RuntimeException         for any other unexpected errors
     */
    @Transactional
    public void deleteImageByProject(String imageId, ObjectId projectId) {
        logger.info("Deleting image by image ID: {} and project ID: {}", imageId, projectId);
        validateImageId(imageId);
        validateObjectId(projectId, "Project ID");

        try {
            imageRepository.findByImageIdAndProject_ProjectID(imageId, projectId)
                    .orElseThrow(() -> {
                        logger.error("Image not found with ID: {} in project: {}", imageId, projectId);
                        return new IllegalArgumentException(
                                "Image not found with ID: " + imageId + " in project: " + projectId);
                    });
            geeResultsRepository.deleteAllByImage_ImageId(imageId);
            Project project = getProjectById(projectId);
            project.getImages().removeIf(img -> img.getImageId().equals(imageId));
            projectRepository.save(project);
            imageRepository.deleteByImageIdAndProject_ProjectID(imageId, projectId);
            logger.info("Image and GEE results deleted successfully with ID: {} from project: {}", imageId, projectId);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to delete image by image ID: {} and project ID: {}", imageId, projectId, e);
            throw new RuntimeException("Failed to delete image: " + e.getMessage(), e);
        }
    }

    /**
     * Bulk deletes multiple images by their IDs.
     *
     * @param imageIds the list of image IDs to delete
     * @throws IllegalArgumentException if the image IDs list is invalid or contains
     *                                  non-existent IDs
     * @throws RuntimeException         for any other unexpected errors
     */
    @Transactional
    public void bulkDeleteImages(List<String> imageIds) {
        logger.info("Attempting to bulk delete images with IDs: {}", imageIds);
        validateImageIds(imageIds);

        try {
            List<String> invalidIds = imageIds.stream()
                    .filter(id -> !imageRepository.existsById(id))
                    .collect(Collectors.toList());
            if (!invalidIds.isEmpty()) {
                logger.error("Images not found with IDs: {}", invalidIds);
                throw new IllegalArgumentException("Images not found with IDs: " + invalidIds);
            }
            for (String id : imageIds) {
                deleteImage(id); // Reuse deleteImage for cascading GEE deletion
            }
            logger.info("Bulk deletion successful for image IDs: {}", imageIds);
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
     * @param projectId the ID of the project
     * @return the count of images
     * @throws IllegalArgumentException if the project ID is invalid
     * @throws ProjectNotFoundException if the project does not exist
     * @throws RuntimeException         for any other unexpected errors
     */
    public long countImagesByProject(ObjectId projectId) {
        logger.info("Counting images for project ID: {}", projectId);
        validateObjectId(projectId, "Project ID");

        try {
            getProjectById(projectId);
            return imageRepository.countByProject_ProjectID(projectId);
        } catch (ProjectNotFoundException e) {
            logger.error("Project not found for counting images: {}", projectId, e);
            throw e;
        } catch (Exception e) {
            logger.error("Failed to count images for project ID: {}", projectId, e);
            throw new RuntimeException("Failed to count images: " + e.getMessage(), e);
        }
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
            logger.error("Invalid project ID in ImageDTO: {}", imageDTO.getProjectId());
            throw new IllegalArgumentException("Invalid project ID: " + imageDTO.getProjectId());
        }
    }

    /**
     * Validates the image ID.
     *
     * @param id the image ID to validate
     * @throws IllegalArgumentException if the image ID is invalid
     */
    private void validateImageId(String id) {
        validateString(id, "Image ID");
    }

    /**
     * Validates the list of image IDs.
     *
     * @param imageIds the list of image IDs to validate
     * @throws IllegalArgumentException if the list of image IDs is invalid
     */
    private void validateImageIds(List<String> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            logger.error("Image IDs list is null or empty");
            throw new IllegalArgumentException("Image IDs list cannot be null or empty");
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
     * Retrieves a project by its ID.
     *
     * @param projectId the ID of the project
     * @return the project
     * @throws ProjectNotFoundException if the project does not exist
     */
    private Project getProjectById(ObjectId projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> {
                    logger.error("Project not found with ID: {}", projectId);
                    return new ProjectNotFoundException("Project not found with ID: " + projectId);
                });
    }
}
