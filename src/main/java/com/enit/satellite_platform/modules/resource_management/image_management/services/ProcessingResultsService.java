package com.enit.satellite_platform.modules.resource_management.image_management.services;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.enit.satellite_platform.modules.resource_management.image_management.dto.resultsSaveRequest;
import com.enit.satellite_platform.modules.resource_management.image_management.exceptions.ImageNotFoundException;
import com.enit.satellite_platform.modules.resource_management.image_management.models.Image;
import com.enit.satellite_platform.modules.resource_management.image_management.models.ProcessingResults;
import com.enit.satellite_platform.modules.resource_management.image_management.models.ProcessingStatus; 
import com.enit.satellite_platform.modules.resource_management.image_management.repositories.ImageRepository;
import com.enit.satellite_platform.modules.resource_management.image_management.repositories.ResultsRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

    /**
     * Saves a processing results to the database, and associates it with an image if
     * specified. Invalidates the corresponding cache entry using
     * ResourceCacheHandler.
     *
     * @param resultsSaveRequest the processing results to save
     * @return the saved processing results
     * @throws ImageNotFoundException if the image associated with the processing results
     *                                is not found with the given ID
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

        ProcessingResults ProcessingResults = new ProcessingResults();
        ProcessingResults.setData(resultsSaveRequest.getData());
        ProcessingResults.setDate(parseDate(resultsSaveRequest.getDate()));
        ProcessingResults.setType(resultsSaveRequest.getType());
        // Set status, default to COMPLETED if null in request
        ProcessingResults.setStatus(resultsSaveRequest.getStatus() != null ? resultsSaveRequest.getStatus() : ProcessingStatus.COMPLETED);
        ProcessingResults.setImage(image);

        if (image != null) {
            image.getResults().add(ProcessingResults);
            imageRepository.save(image);
        }

        ProcessingResults savedResults = ProcessingResultsRepository.save(ProcessingResults);
        logger.info("ProcessingResults saved successfully with ID: {}", savedResults.getResultsId());

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
        ProcessingResults result = ProcessingResultsRepository.findById(id)
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
        logger.info("Fetching ProcessingResults by image ID: {}", imageId);
        validateString(imageId, "Image ID");
        List<ProcessingResults> results = ProcessingResultsRepository.findByImage_ImageId(imageId)
                .orElse(Collections.emptyList());
        if (results.isEmpty()) {
            logger.warn("No ProcessingResults found for image ID: {}", imageId);
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
        return ProcessingResultsRepository.findAll(pageable);
    }

    /**
     * Deletes processing results by their ID. Invalidates the corresponding cache entry
     * using ResourceCacheHandler.
     *
     * @param id the ID of the processing results to delete
     */
    @Transactional
    public void deleteProcessingResultsById(ObjectId id) {
        logger.info("Deleting ProcessingResults by ID: {}", id);
        validateObjectId(id, "ProcessingResults ID");
        if (!ProcessingResultsRepository.existsById(id)) {
            logger.error("ProcessingResults not found with ID: {}", id);
            throw new IllegalArgumentException("ProcessingResults not found with ID: " + id);
        }
        ProcessingResultsRepository.deleteById(id);
        logger.info("ProcessingResults deleted successfully with ID: {}", id);

        // Removed cache invalidation logic for ResourceCacheHandler
    }

    /**
     * Deletes all processing results associated with an image ID. Invalidates
     * corresponding cache entries using ResourceCacheHandler.
     *
     * @param imageId the ID of the image
     */
    @Transactional
    public void deleteProcessingResultsByImageId(String imageId) {
        logger.info("Deleting all ProcessingResults for image ID: {}", imageId);
        validateString(imageId, "Image ID");
        if (!imageRepository.existsById(imageId)) {
            logger.error("Image not found with ID: {}", imageId);
            throw new ImageNotFoundException("Image not found with ID: " + imageId);
        }
        ProcessingResultsRepository.deleteAllByImage_ImageId(imageId);
        logger.info("All ProcessingResults deleted successfully for image ID: {}", imageId);

        // Removed cache invalidation logic for ResourceCacheHandler
    }

    /**
     * Deletes processing results by image ID and result ID. Invalidates the corresponding
     * cache entry using ResourceCacheHandler.
     *
     * @param imageId the ID of the image
     * @param id      the ID of the processing results
     */
    @Transactional
    public void deleteByImage_ImageIdAndId(String imageId, ObjectId id) {
        logger.info("Deleting ProcessingResults by image ID: {} and ID: {}", imageId, id);
        validateString(imageId, "Image ID");
        validateObjectId(id, "ProcessingResults ID");
        if (!imageRepository.existsById(imageId)) {
            logger.error("Image not found with ID: {}", imageId);
            throw new ImageNotFoundException("Image not found with ID: " + imageId);
        }
        if (!ProcessingResultsRepository.existsByImage_ImageIdAndResultsId(imageId, id)) {
            logger.error("ProcessingResults not found with ID {} for image ID {}", id, imageId);
            throw new IllegalArgumentException("ProcessingResults not found with ID " + id + " for image ID " + imageId);
        }

        ProcessingResultsRepository.deleteByImage_ImageIdAndId(imageId, id);
        logger.info("ProcessingResults deleted successfully by image ID: {} and ID: {}", imageId, id);

        // Removed cache invalidation logic for ResourceCacheHandler
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
     * @throws IllegalArgumentException if the input list is null or empty
     */
    @Transactional
    public List<ProcessingResults> bulkSave(List<resultsSaveRequest> resultsSaveRequests) {
        logger.info("Bulk saving {} ProcessingResults", resultsSaveRequests.size());
        if (resultsSaveRequests == null || resultsSaveRequests.isEmpty()) {
            logger.error("Bulk save request list cannot be null or empty");
            throw new IllegalArgumentException("Bulk save request list cannot be null or empty");
        }

        List<ProcessingResults> resultsList = new ArrayList<>();
        Map<String, Image> imageMap = new HashMap<>();

        for (resultsSaveRequest request : resultsSaveRequests) {
            validateresultsSaveRequest(request);

            Image image = null;
            if (request.getImageId() != null) {
                image = imageMap.computeIfAbsent(request.getImageId(), imgId -> imageRepository.findById(imgId)
                        .orElseThrow(() -> new ImageNotFoundException(
                                "Image not found with ID: " + imgId)));
            }

            ProcessingResults ProcessingResults = new ProcessingResults();
            ProcessingResults.setData(request.getData());
            ProcessingResults.setDate(parseDate(request.getDate()));
            ProcessingResults.setType(request.getType());
            // Set status, default to COMPLETED if null in request
            ProcessingResults.setStatus(request.getStatus() != null ? request.getStatus() : ProcessingStatus.COMPLETED);
            ProcessingResults.setImage(image);

            if (image != null) {
                image.getResults().add(ProcessingResults);
            }
            ProcessingResults savedResult = ProcessingResultsRepository.save(ProcessingResults);
            resultsList.add(savedResult);
        }

        if (!imageMap.isEmpty()) {
            imageRepository.saveAll(imageMap.values());
        }

        logger.info("Bulk save completed successfully with {} results", resultsList.size());

        // Removed cache invalidation logic for ResourceCacheHandler

        return resultsList;
    }

    public Map<String, Object> exportResults(ObjectId resultsId){
        //TODO update this to return a TXT file or something
        return null;
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
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
            return LocalDateTime.parse(dateStr, formatter);
        } catch (Exception e) {
            logger.error("Invalid date format: {}", dateStr, e);
            throw new IllegalArgumentException("Invalid date format: " + dateStr, e);
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
