package com.enit.satellite_platform.modules.resource_management.image_management.services;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.enit.satellite_platform.modules.resource_management.image_management.dto.resultsSaveRequest;
import com.enit.satellite_platform.modules.resource_management.image_management.entities.Image;
import com.enit.satellite_platform.modules.resource_management.image_management.entities.ProcessingResults;
import com.enit.satellite_platform.modules.resource_management.image_management.entities.ProcessingStatus;
import com.enit.satellite_platform.modules.resource_management.image_management.exceptions.ImageNotFoundException;
import com.enit.satellite_platform.modules.resource_management.image_management.repositories.ImageRepository;
import com.enit.satellite_platform.modules.resource_management.image_management.repositories.ResultsRepository;
import com.enit.satellite_platform.modules.resource_management.utils.storage_management.StorageManager; // Import StorageManager
import com.enit.satellite_platform.shared.mapper.ResultsMapper; // Mapper confirmed
import java.util.stream.Collectors; // Keep for mapping

/**
 * Service class for handling operations related to Google Earth Engine (processing)
 * tasks.
 * Uses ProcessingResponseCacheHandler for caching processing processing responses.
 */
@Service
public class ProcessingResultsService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessingResultsService.class);

    @Autowired
    private ResultsRepository ProcessingResultsRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private StorageManager storageManager; // Inject StorageManager

    @Autowired
    private ResultsMapper resultsMapper; // Assuming mapper exists

    @Value("${storage.default-type:gridfs}") // Inject default storage type
    private String defaultStorageType;

    /**
     * Saves a processing results to the database, optionally stores an associated file,
     * and associates it with an image if specified.
     * Invalidates the corresponding cache entry using ResourceCacheHandler.
     * specified. Invalidates the corresponding cache entry using
     * ResourceCacheHandler.
     *
     * @param resultsSaveRequest the processing results to save
     * @return the saved processing results
     * @throws ImageNotFoundException if the image associated with the processing results is not found.
     * @throws RuntimeException if file storage fails.
     */
    @Transactional
    public ProcessingResults save(resultsSaveRequest resultsSaveRequest) {
        logger.info("Saving ProcessingResults for request: {}", resultsSaveRequest);
        validateresultsSaveRequest(resultsSaveRequest);

        Image image = null;
        if (resultsSaveRequest.getImageId() != null) {
            image = imageRepository.findById(resultsSaveRequest.getImageId())
                    .orElseThrow(() -> new ImageNotFoundException(
                            "Image not found with ID: " + resultsSaveRequest.getImageId()));
        }

        ProcessingResults processingResults = new ProcessingResults(); // Renamed variable for clarity
        processingResults.setData(resultsSaveRequest.getData());
        processingResults.setDate(parseDate(resultsSaveRequest.getDate()));
        processingResults.setType(resultsSaveRequest.getType());
        // Set status, default to COMPLETED if null in request
        processingResults.setStatus(resultsSaveRequest.getStatus() != null ? resultsSaveRequest.getStatus() : ProcessingStatus.COMPLETED);
        // processingResults.setImage(image); // Defer setting the image reference

        // --- File Storage Logic ---
        MultipartFile file = resultsSaveRequest.getFile();
        if (file != null && !file.isEmpty()) {
            logger.info("Processing results file found: {}", file.getOriginalFilename());
            try {
                String storageIdentifier = storageManager.store(file, null, defaultStorageType);
                processingResults.setStorageIdentifier(storageIdentifier);
                processingResults.setStorageType(defaultStorageType);
                processingResults.setFileSize(file.getSize());
                logger.info("Processing results file stored with identifier: {} and type: {}", storageIdentifier, defaultStorageType);
            } catch (IOException e) {
                logger.error("Failed to store processing results file: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to store processing results file", e);
            }
        } else {
             logger.info("No processing results file provided in the request.");
        }
        // --- End File Storage Logic ---

        // Save ProcessingResults first to get an ID
        ProcessingResults savedResults = ProcessingResultsRepository.save(processingResults);
        logger.debug("ProcessingResults initially saved with ID: {}", savedResults.getResultsId());

        // Now associate with Image if it exists
        if (image != null) {
            savedResults.setImage(image); // Set the image reference on the saved result

            // Ensure results list is initialized in the Image
            if (image.getResults() == null) {
                image.setResults(new ArrayList<>());
            }
            // Add the *saved* result to the image's list
            // Check if the result is already in the list to avoid duplicates if logic runs unexpectedly
            if (!image.getResults().contains(savedResults)) {
                 image.getResults().add(savedResults);
            }
            imageRepository.save(image); // Save the image with the updated list
            logger.debug("Image {} updated with new ProcessingResults {}", image.getImageId(), savedResults.getResultsId());

            // Save ProcessingResults again to persist the @DBRef link to the Image
            savedResults = ProcessingResultsRepository.save(savedResults);
            logger.debug("ProcessingResults {} updated with Image reference", savedResults.getResultsId());
        } else {
             logger.debug("No associated image, skipping image association steps.");
        }

        logger.info("ProcessingResults processing completed successfully with ID: {}", savedResults.getResultsId());

        // Removed cache invalidation logic for ResourceCacheHandler

        return savedResults;
    }

    /**
     * Retrieves processing results by their ID, using ResourceCacheHandler.
     *
     * @param id the ID of the processing results
     * @return the processing results
     */
    public ProcessingResults getProcessingResultsById(ObjectId id) {
        logger.info("Fetching ProcessingResults by ID: {}", id);
        validateObjectId(id, "ProcessingResults ID");

        // Removed caching logic for ResourceCacheHandler

        // Fetch directly from repository
        logger.info("Fetching ProcessingResults directly from repository for ID: {}", id);
        ProcessingResults result = ProcessingResultsRepository.findByIdAndDeletedFalse(id) // Use findByIdAndDeletedFalse
                .orElseThrow(() -> {
                    logger.error("ProcessingResults not found with ID: {}", id);
                    return new IllegalArgumentException("ProcessingResults not found with ID: " + id);
                });

        return result;
    }

    /**
     * Retrieves processing results by the image ID.
     * (Caching not implemented with ResourceCacheHandler here)
     *
     * @param imageId the ID of the image
     * @return the list of processing results associated with the image
     */
    public List<ProcessingResults> getProcessingResultsByImageId(String imageId) {
        logger.info("Fetching ProcessingResults by image ID string: {}", imageId); // Log clarification
        validateString(imageId, "Image ID");
        // NOTE: ImageRepository uses String ID, so no ObjectId conversion needed here.

        // Fetch the Image entity using the String ID
        Optional<Image> imageOptional = imageRepository.findById(imageId); // Use String imageId

        if (imageOptional.isEmpty()) {
            logger.warn("Image not found with ID: {}", imageId); // Log String ID
            return Collections.emptyList(); // Return empty list if image doesn't exist
        }

        Image image = imageOptional.get();
        logger.debug("Found Image entity: {}. Querying ProcessingResults by Image object.", image.getImageId()); // Log String ID

        // Query ResultsRepository using the fetched Image object and ensuring not deleted
        List<ProcessingResults> results = ProcessingResultsRepository.findByImageAndDeletedFalse(image) // Use findByImageAndDeletedFalse
                .orElse(Collections.emptyList());

        if (results.isEmpty()) {
            logger.warn("No ProcessingResults found for Image: {}", image.getImageId());
        } else {
            logger.info("Found {} ProcessingResults for Image: {}", results.size(), image.getImageId());
        }
        return results;
    }

    /**
     * Retrieves all processing results with pagination.
     * (Caching not implemented with ResourceCacheHandler here)
     *
     * @param pageable the pagination information
     * @return the page of processing results
     */
    public Page<ProcessingResults> getAllProcessingResults(Pageable pageable) {
        logger.info("Fetching all ProcessingResults with pageable: {}", pageable);
        validatePageable(pageable);
        return ProcessingResultsRepository.findByDeletedFalse(pageable); // Use findByDeletedFalse
    }

    /**
     * Deletes processing results by their ID. Invalidates the corresponding cache entry
     * using ResourceCacheHandler.
     *
     * @param id the ID of the processing results to soft delete
     */
    @Transactional
    public void deleteProcessingResultsById(ObjectId id) { // Now performs SOFT delete
        logger.info("Soft deleting ProcessingResults by ID: {}", id);
        validateObjectId(id, "ProcessingResults ID");
        ProcessingResults result = ProcessingResultsRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("ProcessingResults not found for soft delete with ID: {}", id);
                    return new IllegalArgumentException("ProcessingResults not found with ID: " + id);
                });

        if (!result.isDeleted()) {
            result.setDeleted(true);
            result.setDeletedAt(new Date());
            ProcessingResultsRepository.save(result);
            logger.info("ProcessingResults soft deleted successfully with ID: {}", id);
        } else {
            logger.info("ProcessingResults with ID {} was already soft deleted.", id);
        }
        // File is NOT deleted on soft delete
    }

    /**
     * Deletes all processing results associated with an image ID. Invalidates
     * corresponding cache entries using ResourceCacheHandler.
     *
     * @param imageId the ID of the image whose results should be soft deleted
     */
    @Transactional
    public void deleteProcessingResultsByImageId(String imageId) { // Now performs SOFT delete
        logger.info("Soft deleting all ProcessingResults for image ID: {}", imageId);
        validateString(imageId, "Image ID");
        if (!imageRepository.existsById(imageId)) {
            logger.error("Image not found with ID: {}", imageId);
            throw new ImageNotFoundException("Image not found with ID: " + imageId);
        }
        List<ProcessingResults> results = ProcessingResultsRepository.findByImage_ImageIdAndDeletedFalse(imageId) // Find only non-deleted results
                .orElse(Collections.emptyList());

        if (!results.isEmpty()) {
            Date deletedAt = new Date();
            for (ProcessingResults result : results) {
                result.setDeleted(true);
                result.setDeletedAt(deletedAt);
            }
            ProcessingResultsRepository.saveAll(results);
            logger.info("Soft deleted {} ProcessingResults for image ID: {}", results.size(), imageId);
        } else {
            logger.info("No active ProcessingResults found to soft delete for image ID: {}", imageId);
        }
        // Files are NOT deleted on soft delete
    }

    /**
     * Deletes processing results by image ID and result ID. Invalidates the corresponding
     * cache entry using ResourceCacheHandler.
     *
     * @param imageId the ID of the image
     * @param id      the ID of the processing results to soft delete
     */
    @Transactional
    public void deleteByImage_ImageIdAndId(String imageId, ObjectId id) { // Now performs SOFT delete
        logger.info("Soft deleting ProcessingResults by image ID: {} and ID: {}", imageId, id);
        validateString(imageId, "Image ID");
        validateObjectId(id, "ProcessingResults ID");

        ProcessingResults result = ProcessingResultsRepository.findByImage_ImageIdAndResultsId(imageId, id)
                .orElseThrow(() -> {
                     logger.error("ProcessingResults not found with ID {} for image ID {}", id, imageId);
                     return new IllegalArgumentException("ProcessingResults not found with ID " + id + " for image ID " + imageId);
                });

         if (!result.isDeleted()) {
            result.setDeleted(true);
            result.setDeletedAt(new Date());
            ProcessingResultsRepository.save(result);
            logger.info("ProcessingResults soft deleted successfully by image ID: {} and ID: {}", imageId, id);
        } else {
            logger.info("ProcessingResults with ID {} for image ID {} was already soft deleted.", id, imageId);
        }
         // File is NOT deleted on soft delete
    }

    /**
     * Updates a processing results with the given ID and request body. Invalidates the
     * corresponding cache entry using ResourceCacheHandler.
     *
     * @param id            the ID of the processing results to update
     * @param updateRequest the request body containing the updated processing results
     *                      information
     * @return the updated processing results
     * @throws IllegalArgumentException if the processing results is not found with the
     *                                  given ID
     * @throws ImageNotFoundException   if the image associated with the processing results
     *                                  is not found with the given ID
     */
    @Transactional
    public ProcessingResults updateProcessingResults(ObjectId id, resultsSaveRequest updateRequest) {
        logger.info("Updating ProcessingResults with ID: {}", id);
        validateObjectId(id, "ProcessingResults ID");
        validateresultsSaveRequest(updateRequest);

        // Fetch existing results - this will use the cache if available via
        // getProcessingResultsById
        ProcessingResults existingResults = getProcessingResultsById(id);

        Image image = null;
        if (updateRequest.getImageId() != null) {
            boolean imageChanged = existingResults.getImage() == null
                    || !existingResults.getImage().getImageId().equals(updateRequest.getImageId());
            image = imageRepository.findById(updateRequest.getImageId())
                    .orElseThrow(() -> {
                        logger.error("Image not found with ID: {}", updateRequest.getImageId());
                        return new ImageNotFoundException("Image not found with ID: " + updateRequest.getImageId());
                    });
            if (imageChanged && existingResults.getImage() != null) {
                Image oldImage = existingResults.getImage();
                oldImage.getResults().remove(existingResults);
                imageRepository.save(oldImage);
            }
        } else {
            if (existingResults.getImage() != null) {
                Image oldImage = existingResults.getImage();
                oldImage.getResults().remove(existingResults);
                imageRepository.save(oldImage);
            }
        }

        existingResults.setData(updateRequest.getData());
        existingResults.setDate(parseDate(updateRequest.getDate()));
        existingResults.setType(updateRequest.getType());
        // Update status, default to COMPLETED if null in request
        existingResults.setStatus(updateRequest.getStatus() != null ? updateRequest.getStatus() : ProcessingStatus.COMPLETED);
        existingResults.setImage(image);

        if (image != null && !image.getResults().contains(existingResults)) {
            image.getResults().add(existingResults);
            imageRepository.save(image);
        }

        ProcessingResults updatedResults = ProcessingResultsRepository.save(existingResults);
        logger.info("ProcessingResults updated successfully with ID: {}", updatedResults.getResultsId());

        // Removed cache invalidation logic for ResourceCacheHandler

        return updatedResults;
    }

    /**
     * Bulk saves multiple processing results in a single database transaction. Invalidates
     * corresponding cache entries using ResourceCacheHandler.
     *
     * @param resultsSaveRequests the list of processing results to save
     * @return the list of saved processing results
     * @throws IllegalArgumentException if the input list is null or empty, or if file processing fails.
     * @throws IOException if storing a file fails.
     */
    @Transactional
    public List<ProcessingResults> bulkSave(List<resultsSaveRequest> resultsSaveRequests, Map<String, MultipartFile> files) throws IOException { // Add files map and throws IOException
        logger.info("Bulk saving {} ProcessingResults with {} potential files", resultsSaveRequests.size(), files.size());
        if (resultsSaveRequests == null || resultsSaveRequests.isEmpty()) {
            logger.error("Bulk save metadata list cannot be null or empty");
            throw new IllegalArgumentException("Bulk save metadata list cannot be null or empty");
        }

        List<ProcessingResults> resultsList = new ArrayList<>();
        Map<String, Image> imageMap = new HashMap<>(); // To optimize image fetching

        for (int i = 0; i < resultsSaveRequests.size(); i++) { // Iterate with index
            resultsSaveRequest request = resultsSaveRequests.get(i);
            validateresultsSaveRequest(request);

            Image image = null;
            if (request.getImageId() != null) {
                image = imageMap.computeIfAbsent(request.getImageId(), imgId -> imageRepository.findById(imgId)
                        .orElseThrow(() -> new ImageNotFoundException(
                                "Image not found with ID: " + imgId)));
            }

            ProcessingResults processingResults = new ProcessingResults(); // Renamed variable
            processingResults.setData(request.getData());
            processingResults.setDate(parseDate(request.getDate()));
            processingResults.setType(request.getType());
            // Set status, default to COMPLETED if null in request
            processingResults.setStatus(request.getStatus() != null ? request.getStatus() : ProcessingStatus.COMPLETED);
            // processingResults.setImage(image); // Defer setting image

            // --- File Storage Logic for Bulk ---
            String fileKey = "file_" + i; // Construct the expected file key
            MultipartFile file = files.get(fileKey);

            if (file != null && !file.isEmpty()) {
                logger.info("Processing results file found for index {}: {}", i, file.getOriginalFilename());
                try {
                    String storageIdentifier = storageManager.store(file, null, defaultStorageType);
                    processingResults.setStorageIdentifier(storageIdentifier);
                    processingResults.setStorageType(defaultStorageType);
                    processingResults.setFileSize(file.getSize());
                    logger.info("Processing results file for index {} stored with identifier: {}", i, storageIdentifier);
                } catch (IOException e) {
                    logger.error("Failed to store processing results file for index {}: {}", i, e.getMessage(), e);
                    throw new IOException("Failed to store processing results file for entry " + i, e);
                }
            } else {
                 logger.debug("No processing results file provided for index {}.", i);
            }
            // --- End File Storage Logic for Bulk ---

            // Save the entity *first*
            ProcessingResults savedResult = ProcessingResultsRepository.save(processingResults);

            // Associate with image *after* saving result
            if (image != null) {
                 savedResult.setImage(image); // Set image on saved result
                 if (image.getResults() == null) {
                    image.setResults(new ArrayList<>());
                 }
                 // Check if the result is already in the list
                 if (!image.getResults().contains(savedResult)) {
                    image.getResults().add(savedResult);
                 }
                 // Note: Image will be saved later in bulk
            }
            resultsList.add(savedResult); // Add the potentially updated savedResult
        }

        // Save all modified images once
        if (!imageMap.isEmpty()) {
            imageRepository.saveAll(imageMap.values());
        }

        // Save all results again to persist the image references
        List<ProcessingResults> finalResults = (List<ProcessingResults>) ProcessingResultsRepository.saveAll(resultsList);

        logger.info("Bulk save completed successfully with {} results", finalResults.size());

        // Removed cache invalidation logic for ResourceCacheHandler

        return finalResults;
    }

    /**
     * Permanently deletes a ProcessingResults entity and its associated stored file.
     *
     * @param id The ID of the ProcessingResults to permanently delete.
     * @throws IllegalArgumentException if the result is not found.
     * @throws RuntimeException if deleting the stored file fails.
     */
    @Transactional
    public void permanentlyDeleteProcessingResults(ObjectId id) {
        logger.warn("Attempting to permanently delete ProcessingResults with ID: {}", id);
        validateObjectId(id, "ProcessingResults ID");

        ProcessingResults result = ProcessingResultsRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("ProcessingResults not found for permanent deletion with ID: {}", id);
                    return new IllegalArgumentException("ProcessingResults not found with ID: " + id);
                });

        // Delete associated file from storage, if it exists
        if (result.getStorageIdentifier() != null && result.getStorageType() != null) {
            try {
                storageManager.delete(result.getStorageIdentifier(), result.getStorageType());
                logger.info("Permanently deleted stored file for ProcessingResults ID: {} (Identifier: {}, Type: {})",
                        id, result.getStorageIdentifier(), result.getStorageType());
            } catch (IOException e) {
                logger.error("Failed to delete stored file for ProcessingResults ID {}: {}", id, e.getMessage(), e);
                // Decide whether to proceed with DB deletion or throw. Throwing for safety.
                throw new RuntimeException("Failed to delete stored file for ProcessingResults ID " + id, e);
            }
        } else {
             logger.info("No stored file associated with ProcessingResults ID {} to delete.", id);
        }

        // Delete the entity from the database
        ProcessingResultsRepository.deleteById(id);
        logger.info("ProcessingResults entity permanently deleted successfully with ID: {}", id);

        // Note: We don't need to remove the reference from the Image entity here,
        // as that relationship is managed via @DBRef and the result document is gone.
    }

    /**
     * Restores a soft-deleted ProcessingResults entity.
     *
     * @param id The ID of the ProcessingResults to restore.
     * @return The restored ProcessingResults entity.
     * @throws IllegalArgumentException if the result is not found.
     * @throws IllegalStateException if the result is not currently soft-deleted.
     */
    @Transactional
    public ProcessingResults restoreProcessingResults(ObjectId id) {
        logger.info("Attempting to restore ProcessingResults with ID: {}", id);
        validateObjectId(id, "ProcessingResults ID");

        ProcessingResults result = ProcessingResultsRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("ProcessingResults not found for restore with ID: {}", id);
                    return new IllegalArgumentException("ProcessingResults not found with ID: " + id);
                });

        if (!result.isDeleted()) {
            logger.warn("Attempted to restore ProcessingResults with ID {} which is not deleted.", id);
            throw new IllegalStateException("ProcessingResults is not deleted.");
        }

        result.setDeleted(false);
        result.setDeletedAt(null);
        ProcessingResults restoredResult = ProcessingResultsRepository.save(result);
        logger.info("ProcessingResults restored successfully with ID: {}", id);
        return restoredResult;
    }

     /**
     * Retrieves the stored file associated with a ProcessingResults entity.
     *
     * @param id The ID of the ProcessingResults.
     * @return A MultipartFile representing the stored data.
     * @throws IllegalArgumentException if the result is not found.
     * @throws RuntimeException if retrieving the file fails or if no file is associated.
     */
    public MultipartFile getProcessingResultFile(ObjectId id) {
        logger.info("Retrieving stored file for ProcessingResults ID: {}", id);
        validateObjectId(id, "ProcessingResults ID");

        ProcessingResults result = ProcessingResultsRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("ProcessingResults not found for file retrieval with ID: {}", id);
                    return new IllegalArgumentException("ProcessingResults not found with ID: " + id);
                });

        if (result.getStorageIdentifier() == null || result.getStorageType() == null) {
            logger.warn("No stored file associated with ProcessingResults ID: {}", id);
            throw new RuntimeException("No stored file associated with ProcessingResults ID: " + id);
        }

        try {
            InputStream inputStream = storageManager.retrieve(result.getStorageIdentifier(), result.getStorageType());
            // Construct a filename (e.g., using ID and original extension if stored, or default)
            // For simplicity, using ID + generic extension. A better approach might store original filename/type.
            String filename = result.getResultsId().toString() + "_result_file";
            // Determine content type - ideally stored with the result, fallback needed
            String contentType = "application/octet-stream"; // Fallback

            return new MockMultipartFile(
                    filename,
                    filename,
                    contentType,
                    inputStream
            );
        } catch (IOException e) {
            logger.error("Failed to retrieve stored file for ProcessingResults ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve stored file for ProcessingResults ID " + id, e);
        }
    }

    /**
     * Force permanently deletes a ProcessingResults entity, regardless of its soft-delete status.
     * This bypasses the soft-delete mechanism entirely.
     *
     * @param id The ID of the ProcessingResults to force delete.
     * @throws IllegalArgumentException if the result is not found.
     */
    @Transactional
    public void forceDeleteProcessingResults(ObjectId id) {
        logger.warn("Attempting to FORCE permanently delete ProcessingResults with ID: {}", id);
        validateObjectId(id, "ProcessingResults ID");

        // Check if exists before calling permanent delete (which also checks)
        // This provides a slightly clearer log message if it doesn't exist at all.
        if (!ProcessingResultsRepository.existsById(id)) {
             logger.error("ProcessingResults not found for force deletion with ID: {}", id);
             throw new IllegalArgumentException("ProcessingResults not found with ID: " + id);
        }

        permanentlyDeleteProcessingResults(id); // Call the permanent delete logic
        logger.info("ProcessingResults force deleted successfully: {}", id);
    }

    public Map<String, Object> exportResults(ObjectId resultsId){
        //TODO update this to return a TXT file or something
        return null;
    }

     /**
     * Retrieves a paginated list of soft-deleted processing results.
     *
     * @param pageable Pagination information.
     * @return A Page of soft-deleted ProcessingResults.
     */
    public Page<ProcessingResults> getDeletedProcessingResults(Pageable pageable) {
        logger.info("Fetching soft-deleted ProcessingResults with pageable: {}", pageable);
        validatePageable(pageable);
        // Assuming ResultsRepository has findByDeletedTrue(Pageable pageable) method
        return ProcessingResultsRepository.findByDeletedTrue(pageable);
    }

    /**
     * Retrieves non-deleted processing results by the project ID.
     *
     * @param projectId the ID of the project
     * @return the list of processing results save requests (DTOs) associated with the project
     */
    public List<resultsSaveRequest> getResultsByProjectId(String projectId) {
        logger.info("Fetching ProcessingResults by project ID: {}", projectId);
        validateString(projectId, "Project ID");

        // Use the custom aggregation query method to find non-deleted results by project ID
        List<ProcessingResults> results = ProcessingResultsRepository.findByProjectIdAndDeletedFalseCustom(projectId);

        if (results.isEmpty()) {
            logger.warn("No active ProcessingResults found for Project ID: {}", projectId);
            return Collections.emptyList();
        } else {
            logger.info("Found {} active ProcessingResults for Project ID: {}", results.size(), projectId);
            // Map entities to resultsSaveRequest DTOs using the existing mapper method
            return results.stream()
                          .map(resultsMapper::toDTO)
                          .collect(Collectors.toList());
        }
    }

    // --- Validation and Helper Methods ---

    private void validateresultsSaveRequest(resultsSaveRequest request) {
        if (request == null || request.getData() == null || request.getDate() == null || request.getType() == null) {
            logger.error("Invalid resultsSaveRequest: {}", request);
            throw new IllegalArgumentException("resultsSaveRequest, data, date, and type cannot be null");
        }
    }

    private LocalDateTime parseDate(String dateStr) {
        try {
            // Consider making the formatter more flexible or using standard ISO format if possible
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
            return LocalDateTime.parse(dateStr, formatter);
        } catch (Exception e) {
             try {
                 // Fallback for ISO_LOCAL_DATE_TIME if the first pattern fails
                 return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
             } catch (Exception e2) {
                 logger.error("Invalid date format: {}", dateStr, e2);
                 throw new IllegalArgumentException("Invalid date format: " + dateStr, e2);
             }
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

    private void validatePageable(Pageable pageable) {
        if (pageable == null || pageable.getPageNumber() < 0 || pageable.getPageSize() <= 0) {
            logger.error("Invalid pageable: {}", pageable);
            throw new IllegalArgumentException("Pageable must be valid with non-negative page and positive size");
        }
    }
}
