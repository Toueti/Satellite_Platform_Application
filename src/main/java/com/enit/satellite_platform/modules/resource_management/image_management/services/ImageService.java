package com.enit.satellite_platform.modules.resource_management.image_management.services;

import com.enit.satellite_platform.exceptions.DuplicationException;
import com.enit.satellite_platform.exceptions.ResourceNotFoundException;
import com.enit.satellite_platform.modules.project_management.entities.PermissionLevel;
import com.enit.satellite_platform.modules.project_management.entities.Project;
import com.enit.satellite_platform.modules.project_management.exceptions.ProjectNotFoundException;
import com.enit.satellite_platform.modules.project_management.repositories.ProjectRepository;
import com.enit.satellite_platform.modules.resource_management.image_management.dto.ImageDTO;
import com.enit.satellite_platform.modules.resource_management.image_management.entities.Image;
import com.enit.satellite_platform.modules.resource_management.image_management.mapper.ImageMapper;
import com.enit.satellite_platform.modules.resource_management.image_management.repositories.ImageRepository;
import com.enit.satellite_platform.modules.resource_management.image_management.repositories.ImageRepository.ImageMetadataProjection;
import com.enit.satellite_platform.modules.resource_management.utils.storage_management.StorageManager;
import com.enit.satellite_platform.modules.resource_management.image_management.repositories.ResultsRepository;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.User;
import com.enit.satellite_platform.modules.user_management.normal_user_service.repositories.UserRepository;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private UserRepository userRepository;

    @Autowired
    private ImageMapper imageMapper;

    @Autowired
    private StorageManager storageManager;

    @Transactional
    public ImageDTO addImage(ImageDTO imageDTO, String storageType) {
        logger.info("Attempting to add image: {} with storage type: {}", imageDTO, storageType);
        validateImageDTO(imageDTO);
        ObjectId projectId = new ObjectId(imageDTO.getProjectId());
        if (imageRepository.existsByNameAndProject_Id(imageDTO.getImageName(), projectId)) {
            logger.warn("Image with name {} already exists in project {}", imageDTO.getImageId(), projectId);
            throw new DuplicationException(
                    "An image with the same name as '" + imageDTO.getImageName() + "' already exists in this project. Consider changing the image name to something else");
        }

        try {
            // Store file using StorageManager
            MultipartFile file = imageDTO.getFile();
            String storageIdentifier = null;
            if (file != null && !file.isEmpty()) {
                storageIdentifier = storageManager.store(file, null, storageType); // Metadata can be passed if needed
            }

            // Map DTO to entity
            Image image = imageMapper.toEntity(imageDTO);
            Project project = getProjectById(projectId);
            image.setProject(project);
            image.setStorageIdentifier(storageIdentifier); // Set generic storage identifier
            image.setStorageType(storageType); // Set storage type
            image.setFileSize(file != null ? file.getSize() : 0);
            image.setRequestTime(new Date());
            image.setUpdatedAt(new Date());

            image = imageRepository.save(image);
            // Ensure the images set is initialized before adding to it
            if (project.getImages() == null) {
                project.setImages(new java.util.HashSet<>());
            }
            project.getImages().add(image);
            projectRepository.save(project);

            logger.info("Image added successfully with Id: {}", image.getImageId());
            return imageMapper.toDTO(image);
        } catch (IOException e) {
            logger.error("Failed to store image with storage type: {}", storageType, e);
            throw new RuntimeException("Failed to store image: " + e.getMessage(), e);
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

        if (!image.getProject().getId().equals(projectId.toString())) {
            logger.warn("Image {} does not belong to project {}", imageId, projectId);
            throw new IllegalArgumentException("This image does not belong to the specified project.");
        }

        Optional<Image> existingImage = imageRepository.findByNameAndProject_Id(newName, projectId);
        if (existingImage.isPresent() && !existingImage.get().getImageId().equals(imageId)) {
            logger.warn("Image name '{}' already exists in project '{}'", newName, projectId);
            throw new DuplicationException(
                    "An image with the name '" + newName + "' already exists in this project.");
        }

        image.setImageName(newName);
        image.setUpdatedAt(new Date());
        Image updatedImage = imageRepository.save(image);
        logger.info("Image renamed successfully to: {}", newName);
        return updatedImage;
    }

    @Transactional
    public void deleteImage(String id) { // Now performs SOFT delete
        logger.info("Attempting to soft delete image with Id: {}", id);
        validateImageId(id);

        Image image = imageRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Image not found for soft delete with Id: {}", id);
                    return new ResourceNotFoundException("Image not found with Id: " + id);
                });

        try {
            image.setDeleted(true);
            image.setDeletedAt(new Date());
            imageRepository.save(image);
            logger.info("Image soft deleted successfully with Id: {}", id);
            // Note: Associated file in StorageManager and ProcessingResults are NOT deleted here.
        } catch (Exception e) {
            logger.error("Failed to soft delete image with Id: {}", id, e);
            throw new RuntimeException("Failed to soft delete image: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void permanentlyDeleteImage(String id) {
        logger.info("Attempting to permanently delete image with Id: {}", id);
        validateImageId(id);

        Image image = imageRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Image already permanently deleted or never existed: {}", id);
                    return new ResourceNotFoundException("Image not found with Id: " + id);
                });

        try {
            // 1. Delete file using StorageManager
            if (image.getStorageIdentifier() != null) {
                storageManager.delete(image.getStorageIdentifier(), image.getStorageType());
                logger.info("Permanently deleted file with identifier: {} and type: {}", image.getStorageIdentifier(), image.getStorageType());
            }

            // 2. Delete associated ProcessingResults
            geeResultsRepository.deleteAllByImage_ImageId(id);
            logger.info("Permanently deleted ProcessingResults associated with image Id: {}", id);

            // 3. Remove image reference from Project
            Project project = image.getProject();
            if (project != null) {
                // Need to fetch the project again to ensure the collection is loaded if lazy
                Project managedProject = projectRepository.findById(new ObjectId(project.getId())).orElse(null);
                if (managedProject != null) {
                    boolean removed = managedProject.getImages().removeIf(img -> img.getImageId().equals(id));
                    if (removed) {
                        projectRepository.save(managedProject);
                        logger.info("Removed image reference Id: {} from project Id: {}", id, managedProject.getId());
                    }
                }
            }

            // 4. Delete the Image document itself
            imageRepository.deleteById(id);
            logger.info("Image document permanently deleted successfully with Id: {}", id);
        } catch (IOException e) {
            logger.error("Failed to delete image file during permanent deletion for Id: {}", id, e);
            // Continue deletion process even if file deletion fails? Or throw? For now, throw.
            throw new RuntimeException("Failed to delete image file during permanent deletion: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Failed to permanently delete image with Id: {}", id, e);
            throw new RuntimeException("Failed to permanently delete image: " + e.getMessage(), e);
        }
    }

    @Transactional
    public ImageDTO restoreImage(String id) {
        logger.info("Attempting to restore image with Id: {}", id);
        validateImageId(id);

        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found with Id: " + id));

        if (!image.isDeleted()) {
            throw new IllegalStateException("Image is not deleted.");
        }

        // Optional: Check if the parent project is also deleted?
        // If project is deleted, maybe image shouldn't be restorable independently?
        // For now, allow independent restore.

        image.setDeleted(false);
        image.setDeletedAt(null);
        Image restoredImage = imageRepository.save(image);
        logger.info("Image restored successfully: {}", id);
        return imageMapper.toDTO(restoredImage);
    }

     @Transactional
    public void forceDeleteImage(String id, String userEmail) {
        logger.info("Force deleting image ID: {} by user: {}", id, userEmail);
        validateImageId(id);
        validateString(userEmail, "User Email");

        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found with ID: " + id));

        Project project = image.getProject();
        if (project == null) {
             logger.warn("Image {} has no associated project, proceeding with force delete.", id);
             // Allow deletion if no project link? Or throw error? For now, allow.
        } else {
            User user = getUserByEmail(userEmail, "User not found");
             if (!project.getOwner().equals(user)) { // Only project owner can force delete
                 logger.error("Access denied for user {} to force delete image {}", userEmail, id);
                 throw new AccessDeniedException("Only the project owner can force delete images within their project.");
             }
        }

        // No check for isDeleted needed for force delete

        permanentlyDeleteImage(id); // Call the permanent delete logic
        logger.info("Image force deleted successfully: {}", id);
    }

    /**
     * Retrieves all images with the given pageable parameters.
     * 
     * @param pageable The pageable parameters to use for the query.
     * @return A page of ImageDTO objects, containing only the metadata of the images.
     */
    public Page<ImageDTO> getAllImages(Pageable pageable) {
        logger.info("Retrieving all images with pageable: {}", pageable);
        validatePageable(pageable);

        try {
            Page<ImageMetadataProjection> page = imageRepository.findAllProjectedBy(pageable);
            List<ImageDTO> dtoList = imageMapper.projectionToDTOList(page.getContent());
            return new PageImpl<>(dtoList, pageable, page.getTotalElements());
        } catch (Exception e) {
            logger.error("Failed to retrieve images", e);
            throw new RuntimeException("Failed to retrieve images: " + e.getMessage(), e);
        }
    }

    public Optional<ImageDTO> getImageByName(String name, ObjectId projectId) {
        logger.info("Retrieving image by name: {} and projectId: {}", name, projectId);
        validateString(name, "Image name");
        validateObjectId(projectId, "Project Id");

        try {
            return imageRepository.findByImageNameAndProject_Id(name, projectId)
                    .map(imageMapper::toDTO);
        } catch (Exception e) {
            logger.error("Failed to retrieve image by name and project", e);
            throw new RuntimeException("Failed to retrieve image: " + e.getMessage(), e);
        }
    }

    public ImageDTO getImageById(String id) {
        logger.info("Retrieving image by Id: {}", id);
        validateImageId(id);

        try {
            return imageRepository.findProjectedByImageId(id)
                    .map(imageMapper::toDTO)
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

    public MultipartFile getImageData(String id) {
        logger.info("Retrieving image data for Id: {}", id);
        validateImageId(id);

        try {
            Image image = imageRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Image not found with Id: " + id));
            if (image.getStorageIdentifier() == null) {
                logger.warn("No stored file associated with image Id: {}", id);
                return null;
            }

            InputStream inputStream = storageManager.retrieve(image.getStorageIdentifier(), image.getStorageType());
            return new MockMultipartFile(
                    image.getImageName(),
                    image.getImageName(),
                    "application/octet-stream", // Adjust content type as needed
                    inputStream
            );
        } catch (IOException e) {
            logger.error("Failed to retrieve image data for Id: {}", id, e);
            throw new RuntimeException("Failed to retrieve image data: " + e.getMessage(), e);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to retrieve image data for Id: {}", id, e);
            throw new RuntimeException("Failed to retrieve image data: " + e.getMessage(), e);
        }
    }

    public Page<ImageDTO> getImagesByProject(ObjectId projectId, Pageable pageable) {
        logger.info("Retrieving images by project Id: {} with pageable: {}", projectId, pageable);
        validateObjectId(projectId, "Project Id");
        validatePageable(pageable);

        try {
            getProjectById(projectId); // Ensure project exists
            Page<ImageMetadataProjection> page = imageRepository.findAllByProject_IdProjectedBy(projectId, pageable);
            List<ImageDTO> dtoList = imageMapper.projectionToDTOList(page.getContent());
            return new PageImpl<>(dtoList, pageable, page.getTotalElements());
        } catch (Exception e) {
            logger.error("Failed to retrieve images by project Id: {}", projectId, e);
            throw new RuntimeException("Failed to retrieve images: " + e.getMessage(), e);
        }
    }

    // Renamed to reflect soft delete behavior
    @Transactional
    public void softDeleteAllImagesByProject(ObjectId projectId) {
        logger.info("Soft deleting all images for project Id: {}", projectId);
        validateObjectId(projectId, "Project Id");

        try {
            List<Image> images = imageRepository.findAllByProject_Id(projectId);
            Date deletedAt = new Date();
            for (Image image : images) {
                if (!image.isDeleted()) { // Avoid re-deleting
                    image.setDeleted(true);
                    image.setDeletedAt(deletedAt);
                    imageRepository.save(image); // Save each image individually
                }
            }
            logger.info("All images soft deleted successfully for project Id: {}", projectId);
        } catch (Exception e) {
            logger.error("Failed to soft delete images for project Id: {}", projectId, e);
            throw new RuntimeException("Failed to soft delete images: " + e.getMessage(), e);
        }
    }

    // New method for permanent deletion cascade from project
    @Transactional
    public void permanentlyDeleteAllImagesByProject(ObjectId projectId) {
        logger.info("Permanently deleting all images for project Id: {}", projectId);
        validateObjectId(projectId, "Project Id");
        List<Image> images = imageRepository.findAllByProject_Id(projectId);
        for (Image image : images) {
            try {
                permanentlyDeleteImage(image.getImageId()); // Reuse existing permanent delete logic
            } catch (Exception e) {
                logger.error("Failed to permanently delete image {} during cascade delete for project {}", image.getImageId(), projectId, e);
                // Decide whether to continue or re-throw
            }
        }
         logger.info("Finished permanent deletion cascade for images in project {}", projectId);
    }

     // New method for restore cascade from project
    @Transactional
    public void restoreAllImagesByProject(ObjectId projectId) {
        logger.info("Restoring all images for project Id: {}", projectId);
        validateObjectId(projectId, "Project Id");
        List<Image> images = imageRepository.findAllByProject_Id(projectId);
        for (Image image : images) {
             if (image.isDeleted()) { // Only restore if deleted
                 try {
                     restoreImage(image.getImageId()); // Reuse existing restore logic
                 } catch (Exception e) {
                     logger.error("Failed to restore image {} during cascade restore for project {}", image.getImageId(), projectId, e);
                     // Decide whether to continue or re-throw
                 }
             }
        }
         logger.info("Finished restore cascade for images in project {}", projectId);
    }

    public Optional<ImageDTO> getImageByImageIdAndProject(String imageId, ObjectId projectId) {
        logger.info("Retrieving image by image Id: {} and project Id: {}", imageId, projectId);
        validateImageId(imageId);
        validateObjectId(projectId, "Project Id");

        try {
            return imageRepository.findByImageIdAndProject_Id(imageId, projectId)
                    .map(imageMapper::toDTO);
        } catch (Exception e) {
            logger.error("Failed to retrieve image by image Id and project Id", e);
            throw new RuntimeException("Failed to retrieve image: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteImageByProject(String imageId, ObjectId projectId) { // Now performs SOFT delete
        logger.info("Soft deleting image by image Id: {} and project Id: {}", imageId, projectId);
        validateImageId(imageId);
        validateObjectId(projectId, "Project Id");

        try {
            Image image = imageRepository.findByImageIdAndProject_Id(imageId, projectId)
                    .orElseThrow(() -> {
                        logger.error("Image not found with Id: {} in project: {}", imageId, projectId);
                        return new ResourceNotFoundException(
                                "Image not found with Id: " + imageId + " in project: " + projectId);
                    });

            if (!image.isDeleted()) {
                image.setDeleted(true);
                image.setDeletedAt(new Date());
                imageRepository.save(image);
                logger.info("Image soft deleted successfully with Id: {} from project: {}", imageId, projectId);
            } else {
                 logger.info("Image {} in project {} was already soft deleted.", imageId, projectId);
            }
        } catch (ResourceNotFoundException e) {
             throw e;
        } catch (Exception e) {
            logger.error("Failed to soft delete image by image Id: {} and project Id: {}", imageId, projectId, e);
            throw new RuntimeException("Failed to soft delete image: " + e.getMessage(), e);
        }
    }

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
                deleteImage(id); // Calls the soft delete method now
            }
            logger.info("Bulk soft deletion successful for image Ids: {}", imageIds);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to bulk delete images", e);
            throw new RuntimeException("Failed to bulk delete images: " + e.getMessage(), e);
        }
    }

    public long countImagesByProject(ObjectId projectId) {
        logger.info("Counting images for project Id: {}", projectId);
        validateObjectId(projectId, "Project Id");

        try {
            getProjectById(projectId);
            return imageRepository.countByProject_Id(projectId);
        } catch (ProjectNotFoundException e) {
            logger.error("Project not found for counting images: {}", projectId, e);
            throw e;
        } catch (Exception e) {
            logger.error("Failed to count images for project Id: {}", projectId, e);
            throw new RuntimeException("Failed to count images: " + e.getMessage(), e);
        }
    }

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

        if (!sourceProject.hasAccess(user, PermissionLevel.READ)) {
            logger.warn("User {} lacks READ access to source project {}", userEmail, sourceProjectId);
            throw new AccessDeniedException("User does not have read access to the source project.");
        }
        if (!targetProject.hasAccess(user, PermissionLevel.WRITE)) {
            logger.warn("User {} lacks WRITE access to target project {}", userEmail, targetProjectId);
            throw new AccessDeniedException("User does not have write access to the target project.");
        }

        List<Image> sourceImages = imageRepository.findAllById(imageIds);
        if (sourceImages.size() != imageIds.size()) {
            List<String> foundIds = sourceImages.stream().map(Image::getImageId).collect(Collectors.toList());
            List<String> missingIds = new ArrayList<>(imageIds);
            missingIds.removeAll(foundIds);
            logger.error("Some source images not found: {}", missingIds);
            throw new ResourceNotFoundException("Could not find source images with IDs: " + missingIds);
        }

        List<Image> importedImages = new ArrayList<>();
        for (Image sourceImage : sourceImages) {
            if (!sourceImage.getProject().getId().equals(sourceProjId.toString())) {
                logger.error("Image {} does not belong to source project {}", sourceImage.getImageId(), sourceProjId);
                continue;
            }

            Image newImage = new Image();
            String originalName = sourceImage.getImageName();
            String newName = findAvailableName(originalName, targetProjId);

            newImage.setImageName(newName);
            newImage.setStorageIdentifier(sourceImage.getStorageIdentifier()); // Copy storage reference
            newImage.setStorageType(sourceImage.getStorageType()); // Copy storage type
            newImage.setFileSize(sourceImage.getFileSize());
            newImage.setMetadata(sourceImage.getMetadata() != null ? new java.util.HashMap<>(sourceImage.getMetadata()) : null);
            newImage.setProject(targetProject);
            newImage.setRequestTime(new Date());
            newImage.setUpdatedAt(new Date());

            try {
                Image savedImage = imageRepository.save(newImage);
                targetProject.getImages().add(savedImage);
                importedImages.add(savedImage);
                logger.info("Successfully imported image {} as {} to project {}", sourceImage.getImageId(), savedImage.getImageName(), targetProjId);
            } catch (DataIntegrityViolationException e) {
                logger.error("Data integrity violation while saving imported image '{}' to project {}", newName, targetProjId, e);
                throw new RuntimeException("Failed to save imported image due to potential name conflict: " + newName, e);
            }
        }

        projectRepository.save(targetProject);
        logger.info("Finished importing {} images to project {}", importedImages.size(), targetProjId);
        return imageMapper.toDTOList(importedImages);
    }

    private String findAvailableName(String originalName, ObjectId targetProjectId) {
        String currentName = originalName;
        int copyCount = 0;
        while (imageRepository.existsByNameAndProject_Id(currentName, targetProjectId)) {
            copyCount++;
            currentName = originalName + "_copy_" + copyCount;
            if (copyCount > 100) {
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
    }

    private ObjectId parseObjectId(String idString, String fieldName) {
        validateString(idString, fieldName);
        try {
            return new ObjectId(idString);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid {} format: {}", fieldName, idString);
            throw new IllegalArgumentException("Invalid " + fieldName + " format: " + idString);
        }
    }

    private void validateImageId(String id) {
        validateString(id, "Image Id");
    }

    private void validateImageIds(List<String> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            logger.error("Image Ids list is null or empty");
            throw new IllegalArgumentException("Image Ids list cannot be null or empty");
        }
    }

    private void validateString(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            logger.error("{} cannot be null or empty", fieldName);
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
    }

    private void validateObjectId(ObjectId id, String fieldName) {
        if (id == null) {
            logger.error("{} cannot be null", fieldName);
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
    }

    private User getUserByEmail(String email, String errorMessage) {
        validateString(email, "Email");
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error(errorMessage + ": {}", email);
                    return new UsernameNotFoundException(errorMessage + ": " + email);
                });
    }

    private void validatePageable(Pageable pageable) {
        if (pageable == null || pageable.getPageNumber() < 0 || pageable.getPageSize() <= 0) {
            logger.error("Invalid pageable: {}", pageable);
            throw new IllegalArgumentException("Pageable must be valid with non-negative page and positive size");
        }
    }

    private Project getProjectById(ObjectId projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> {
                    logger.error("Project not found with Id: {}", projectId);
                    return new ProjectNotFoundException("Project not found with Id: " + projectId);
                 });
    }

    /**
     * Retrieves soft-deleted images with pagination.
     *
     * @param pageable Pagination information.
     * @return A page of soft-deleted images as ImageDTO.
     */
    public Page<ImageDTO> getDeletedImages(Pageable pageable) {
        logger.info("Fetching soft-deleted images with pageable: {}", pageable);
        validatePageable(pageable);
        Page<Image> deletedImagesPage = imageRepository.findByDeletedTrue(pageable); // Need to add this method to repo
        return imageMapper.toDTOPage(deletedImagesPage); // Assuming ProjectMapper has toDTOPage for Image
    }
}
/* package com.enit.satellite_platform.modules.resource_management.image_management.services;

import com.enit.satellite_platform.exceptions.DuplicationException;
import com.enit.satellite_platform.exceptions.ResourceNotFoundException;
import com.enit.satellite_platform.modules.project_management.entities.PermissionLevel;
import com.enit.satellite_platform.modules.project_management.entities.Project;
import com.enit.satellite_platform.modules.project_management.exceptions.ProjectNotFoundException;
import com.enit.satellite_platform.modules.project_management.repositories.ProjectRepository;
import com.enit.satellite_platform.modules.resource_management.image_management.dto.ImageDTO;
import com.enit.satellite_platform.modules.resource_management.image_management.entities.Image;
import com.enit.satellite_platform.modules.resource_management.image_management.mapper.ImageMapper;
import com.enit.satellite_platform.modules.resource_management.image_management.repositories.ImageRepository;
import com.enit.satellite_platform.modules.resource_management.image_management.repositories.ImageRepository.ImageMetadataProjection;
import com.enit.satellite_platform.modules.resource_management.image_management.repositories.ResultsRepository;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.User;
import com.enit.satellite_platform.modules.user_management.normal_user_service.repositories.UserRepository;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private UserRepository userRepository;

    @Autowired
    private ImageMapper imageMapper;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Transactional
    public ImageDTO addImage(ImageDTO imageDTO) {
        logger.info("Attempting to add image: {}", imageDTO);
        validateImageDTO(imageDTO);
        ObjectId projectId = new ObjectId(imageDTO.getProjectId());
        if (imageRepository.existsByImageIdAndProject_Id(imageDTO.getImageId(), projectId)) {
            logger.warn("Image with Id {} already exists in project {}", imageDTO.getImageId(), projectId);
            throw new DuplicationException(
                    "An image with the id '" + imageDTO.getImageId() + "' already exists in this project.");
        }

        try {
            // Store file in GridFS
            MultipartFile file = imageDTO.getFile();
            String gridFsFileId = null;
            if (file != null && !file.isEmpty()) {
                gridFsFileId = gridFsTemplate.store(
                        file.getInputStream(),
                        file.getOriginalFilename(),
                        file.getContentType()
                ).toString();
            }

            // Map DTO to entity
            Image image = imageMapper.toEntity(imageDTO);
            Project project = getProjectById(projectId);
            image.setProject(project);
            image.setGridFsFileId(gridFsFileId); // Set GridFS file ID
            image.setFileSize(file != null ? file.getSize() : 0);
            image.setRequestTime(new Date());
            image.setUpdatedAt(new Date());

            image = imageRepository.save(image);
            project.getImages().add(image);
            projectRepository.save(project);

            logger.info("Image added successfully with Id: {}", image.getImageId());
            return imageMapper.toDTO(image);
        } catch (IOException e) {
            logger.error("Failed to store image in GridFS", e);
            throw new RuntimeException("Failed to store image in GridFS: " + e.getMessage(), e);
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

        if (!image.getProject().getId().equals(projectId)) {
            logger.warn("Image {} does not belong to project {}", imageId, projectId);
            throw new IllegalArgumentException("This image does not belong to the specified project.");
        }

        Optional<Image> existingImage = imageRepository.findByNameAndProject_Id(newName, projectId);
        if (existingImage.isPresent() && !existingImage.get().getImageId().equals(imageId)) {
            logger.warn("Image name '{}' already exists in project '{}'", newName, projectId);
            throw new DuplicationException(
                    "An image with the name '" + newName + "' already exists in this project.");
        }

        image.setImageName(newName);
        image.setUpdatedAt(new Date());
        Image updatedImage = imageRepository.save(image);
        logger.info("Image renamed successfully to: {}", newName);
        return updatedImage;
    }

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
            // Delete GridFS file
            if (image.getGridFsFileId() != null) {
                gridFsTemplate.delete(new org.springframework.data.mongodb.core.query.Query(
                        org.springframework.data.mongodb.core.query.Criteria.where("_id").is(image.getGridFsFileId())));
                logger.info("Deleted GridFS file with ID: {}", image.getGridFsFileId());
            }

            geeResultsRepository.deleteAllByImage_ImageId(id);
            logger.info("Deleted GeeResults associated with image Id: {}", id);

            Project project = image.getProject();
            if (project != null) {
                project.getImages().removeIf(img -> img.getImageId().equals(id));
                projectRepository.save(project);
                logger.info("Removed image Id: {} from project Id: {}", id, project.getId());
            }

            imageRepository.deleteById(id);
            logger.info("Image deleted successfully with Id: {}", id);
        } catch (Exception e) {
            logger.error("Failed to delete image with Id: {}", id, e);
            throw new RuntimeException("Failed to delete image: " + e.getMessage(), e);
        }
    }

    public Page<ImageDTO> getAllImages(Pageable pageable) {
        logger.info("Retrieving all images with pageable: {}", pageable);
        validatePageable(pageable);

        try {
            Page<ImageMetadataProjection> page = imageRepository.findAllProjectedBy(pageable);
            List<ImageDTO> dtoList = imageMapper.projectionToDTOList(page.getContent());
            return new PageImpl<>(dtoList, pageable, page.getTotalElements());
        } catch (Exception e) {
            logger.error("Failed to retrieve images", e);
            throw new RuntimeException("Failed to retrieve images: " + e.getMessage(), e);
        }
    }

    public Optional<ImageDTO> getImageByName(String name, ObjectId projectId) {
        logger.info("Retrieving image by name: {} and projectId: {}", name, projectId);
        validateString(name, "Image name");
        validateObjectId(projectId, "Project Id");

        try {
            return imageRepository.findByImageNameAndProject_Id(name, projectId)
                    .map(imageMapper::toDTO);
        } catch (Exception e) {
            logger.error("Failed to retrieve image by name and project", e);
            throw new RuntimeException("Failed to retrieve image: " + e.getMessage(), e);
        }
    }

    public ImageDTO getImageById(String id) {
        logger.info("Retrieving image by Id: {}", id);
        validateImageId(id);

        try {
            return imageRepository.findProjectedByImageId(id)
                    .map(imageMapper::toDTO)
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

    public MultipartFile getImageData(String id) {
        logger.info("Retrieving image data for Id: {}", id);
        validateImageId(id);

        try {
            Image image = imageRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Image not found with Id: " + id));
            if (image.getGridFsFileId() == null) {
                logger.warn("No GridFS file associated with image Id: {}", id);
                return null;
            }

            com.mongodb.client.gridfs.model.GridFSFile gridFSFile = gridFsTemplate.findOne(
                    new org.springframework.data.mongodb.core.query.Query(
                            org.springframework.data.mongodb.core.query.Criteria.where("_id").is(image.getGridFsFileId())));
            if (gridFSFile == null) {
                logger.warn("GridFS file not found for ID: {}", image.getGridFsFileId());
                throw new ResourceNotFoundException("GridFS file not found for ID: " + image.getGridFsFileId());
            }

            org.springframework.data.mongodb.gridfs.GridFsResource resource = gridFsTemplate.getResource(gridFSFile);
            return new MockMultipartFile(
                    resource.getFilename(),
                    resource.getFilename(),
                    resource.getContentType(),
                    resource.getInputStream()
            );
        } catch (IOException e) {
            logger.error("Failed to retrieve image data from GridFS for Id: {}", id, e);
            throw new RuntimeException("Failed to retrieve image data: " + e.getMessage(), e);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to retrieve image data for Id: {}", id, e);
            throw new RuntimeException("Failed to retrieve image data: " + e.getMessage(), e);
        }
    }

    public List<ImageDTO> getImagesByProject(ObjectId projectId) {
        logger.info("Retrieving images by project Id: {}", projectId);
        validateObjectId(projectId, "Project Id");

        try {
            getProjectById(projectId);
            List<ImageMetadataProjection> projections = imageRepository.findAllByProject_IdProjectedBy(projectId);
            return imageMapper.projectionToDTOList(projections);
        } catch (Exception e) {
            logger.error("Failed to retrieve images by project Id: {}", projectId, e);
            throw new RuntimeException("Failed to retrieve images: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteAllImagesByProject(ObjectId projectId) {
        logger.info("Deleting all images for project Id: {}", projectId);
        validateObjectId(projectId, "Project Id");

        try {
            Project project = getProjectById(projectId);
            List<Image> images = imageRepository.findAllByProject_Id(projectId);
            for (Image image : images) {
                if (image.getGridFsFileId() != null) {
                    gridFsTemplate.delete(new org.springframework.data.mongodb.core.query.Query(
                            org.springframework.data.mongodb.core.query.Criteria.where("_id").is(image.getGridFsFileId())));
                    logger.info("Deleted GridFS file with ID: {}", image.getGridFsFileId());
                }
                geeResultsRepository.deleteAllByImage_ImageId(image.getImageId());
                logger.info("Deleted GeeResults for image Id: {}", image.getImageId());
            }
            imageRepository.deleteAllByProject_Id(projectId);
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

    public Optional<ImageDTO> getImageByImageIdAndProject(String imageId, ObjectId projectId) {
        logger.info("Retrieving image by image Id: {} and project Id: {}", imageId, projectId);
        validateImageId(imageId);
        validateObjectId(projectId, "Project Id");

        try {
            return imageRepository.findByImageIdAndProject_Id(imageId, projectId)
                    .map(imageMapper::toDTO);
        } catch (Exception e) {
            logger.error("Failed to retrieve image by image Id and project Id", e);
            throw new RuntimeException("Failed to retrieve image: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteImageByProject(String imageId, ObjectId projectId) {
        logger.info("Deleting image by image Id: {} and project Id: {}", imageId, projectId);
        validateImageId(imageId);
        validateObjectId(projectId, "Project Id");

        try {
            Image image = imageRepository.findByImageIdAndProject_Id(imageId, projectId)
                    .orElseThrow(() -> {
                        logger.error("Image not found with Id: {} in project: {}", imageId, projectId);
                        return new IllegalArgumentException(
                                "Image not found with Id: " + imageId + " in project: " + projectId);
                    });
            if (image.getGridFsFileId() != null) {
                gridFsTemplate.delete(new org.springframework.data.mongodb.core.query.Query(
                        org.springframework.data.mongodb.core.query.Criteria.where("_id").is(image.getGridFsFileId())));
                logger.info("Deleted GridFS file with ID: {}", image.getGridFsFileId());
            }
            geeResultsRepository.deleteAllByImage_ImageId(imageId);
            Project project = getProjectById(projectId);
            project.getImages().removeIf(img -> img.getImageId().equals(imageId));
            projectRepository.save(project);
            imageRepository.deleteByImageIdAndProject_Id(imageId, projectId);
            logger.info("Image and GEE results deleted successfully with Id: {} from project: {}", imageId, projectId);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to delete image by image Id: {} and project Id: {}", imageId, projectId, e);
            throw new RuntimeException("Failed to delete image: " + e.getMessage(), e);
        }
    }

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
                deleteImage(id);
            }
            logger.info("Bulk deletion successful for image Ids: {}", imageIds);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to bulk delete images", e);
            throw new RuntimeException("Failed to bulk delete images: " + e.getMessage(), e);
        }
    }

    public long countImagesByProject(ObjectId projectId) {
        logger.info("Counting images for project Id: {}", projectId);
        validateObjectId(projectId, "Project Id");

        try {
            getProjectById(projectId);
            return imageRepository.countByProject_Id(projectId);
        } catch (ProjectNotFoundException e) {
            logger.error("Project not found for counting images: {}", projectId, e);
            throw e;
        } catch (Exception e) {
            logger.error("Failed to count images for project Id: {}", projectId, e);
            throw new RuntimeException("Failed to count images: " + e.getMessage(), e);
        }
    }

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

        if (!sourceProject.hasAccess(user, PermissionLevel.READ)) {
            logger.warn("User {} lacks READ access to source project {}", userEmail, sourceProjectId);
            throw new AccessDeniedException("User does not have read access to the source project.");
        }
        if (!targetProject.hasAccess(user, PermissionLevel.WRITE)) {
            logger.warn("User {} lacks WRITE access to target project {}", userEmail, targetProjectId);
            throw new AccessDeniedException("User does not have write access to the target project.");
        }

        List<Image> sourceImages = imageRepository.findAllById(imageIds);
        if (sourceImages.size() != imageIds.size()) {
            List<String> foundIds = sourceImages.stream().map(Image::getImageId).collect(Collectors.toList());
            List<String> missingIds = new ArrayList<>(imageIds);
            missingIds.removeAll(foundIds);
            logger.error("Some source images not found: {}", missingIds);
            throw new ResourceNotFoundException("Could not find source images with IDs: " + missingIds);
        }

        List<Image> importedImages = new ArrayList<>();
        for (Image sourceImage : sourceImages) {
            if (!sourceImage.getProject().getId().equals(sourceProjId)) {
                logger.error("Image {} does not belong to source project {}", sourceImage.getImageId(), sourceProjId);
                continue;
            }

            Image newImage = new Image();
            String originalName = sourceImage.getImageName();
            String newName = findAvailableName(originalName, targetProjId);

            newImage.setImageName(newName);
            newImage.setGridFsFileId(sourceImage.getGridFsFileId()); // Copy GridFS reference
            newImage.setFileSize(sourceImage.getFileSize());
            newImage.setMetadata(sourceImage.getMetadata() != null ? new java.util.HashMap<>(sourceImage.getMetadata()) : null);
            newImage.setProject(targetProject);
            newImage.setRequestTime(new Date());
            newImage.setUpdatedAt(new Date());

            try {
                Image savedImage = imageRepository.save(newImage);
                targetProject.getImages().add(savedImage);
                importedImages.add(savedImage);
                logger.info("Successfully imported image {} as {} to project {}", sourceImage.getImageId(), savedImage.getImageName(), targetProjId);
            } catch (DataIntegrityViolationException e) {
                logger.error("Data integrity violation while saving imported image '{}' to project {}", newName, targetProjId, e);
                throw new RuntimeException("Failed to save imported image due to potential name conflict: " + newName, e);
            }
        }

        projectRepository.save(targetProject);
        logger.info("Finished importing {} images to project {}", importedImages.size(), targetProjId);
        return imageMapper.toDTOList(importedImages);
    }

    private String findAvailableName(String originalName, ObjectId targetProjectId) {
        String currentName = originalName;
        int copyCount = 0;
        while (imageRepository.existsByNameAndProject_Id(currentName, targetProjectId)) {
            copyCount++;
            currentName = originalName + "_copy_" + copyCount;
            if (copyCount > 100) {
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
    }

    private ObjectId parseObjectId(String idString, String fieldName) {
        validateString(idString, fieldName);
        try {
            return new ObjectId(idString);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid {} format: {}", fieldName, idString);
            throw new IllegalArgumentException("Invalid " + fieldName + " format: " + idString);
        }
    }

    private void validateImageId(String id) {
        validateString(id, "Image Id");
    }

    private void validateImageIds(List<String> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            logger.error("Image Ids list is null or empty");
            throw new IllegalArgumentException("Image Ids list cannot be null or empty");
        }
    }

    private void validateString(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            logger.error("{} cannot be null or empty", fieldName);
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
    }

    private void validateObjectId(ObjectId id, String fieldName) {
        if (id == null) {
            logger.error("{} cannot be null", fieldName);
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
    }

    private User getUserByEmail(String email, String errorMessage) {
        validateString(email, "Email");
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error(errorMessage + ": {}", email);
                    return new UsernameNotFoundException(errorMessage + ": " + email);
                });
    }

    private void validatePageable(Pageable pageable) {
        if (pageable == null || pageable.getPageNumber() < 0 || pageable.getPageSize() <= 0) {
            logger.error("Invalid pageable: {}", pageable);
            throw new IllegalArgumentException("Pageable must be valid with non-negative page and positive size");
        }
    }

    private Project getProjectById(ObjectId projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> {
                    logger.error("Project not found with Id: {}", projectId);
                    return new ProjectNotFoundException("Project not found with Id: " + projectId);
                });
    }
} */
