package com.enit.satellite_platform.resources_management.services;

import com.enit.satellite_platform.resources_management.dto.*;
import com.enit.satellite_platform.resources_management.exception.GeeProcessingException;
import com.enit.satellite_platform.resources_management.exception.ImageNotFoundException;
import com.enit.satellite_platform.resources_management.models.GeeResults;
import com.enit.satellite_platform.resources_management.models.Image;
import com.enit.satellite_platform.resources_management.repositories.GeeResultsRepository;
import com.enit.satellite_platform.resources_management.repositories.ImageRepository;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service class for handling operations related to Google Earth Engine (GEE)
 * tasks.
 * This class provides methods to start asynchronous tasks, retrieve task
 * progress,
 * save and retrieve GEE results, and manage GEE results associated with images.
 * It interacts with a Flask backend for processing GEE requests and uses Redis
 * for caching task progress.
 */
@Service
public class GeeService {

    private static final Logger logger = LoggerFactory.getLogger(GeeService.class);
    private static final String TASK_CACHE_PREFIX = "task:";
    private static final String REQUEST_CACHE_PREFIX = "request:";

    @Autowired
    private GeeResultsRepository geeResultsRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RedisTemplate<String, GeeResponse> redisTemplate;

    @Autowired
    private RedisTemplate<String, GeeRequest> redisRequestTemplate;

    @Value("${python.backend.url}")
    private String flaskBaseUrl;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Starts an asynchronous task for the given GEE request. The task is executed
     * in a separate thread and the task ID is returned immediately. The task ID can
     * be used to retrieve the task progress using the getTaskProgress method.
     *
     * @param request the GEE request to process
     * @return the ID of the started task
     */
    @Async("asyncTaskExecutor")
    public String startAsyncTask(GeeRequest request) {
        String taskId = UUID.randomUUID().toString();
        logger.info("Starting async task with ID: {} for request: {}", taskId, request);
        
        // Store the original request in Redis
        redisRequestTemplate.opsForValue().set(REQUEST_CACHE_PREFIX + taskId, request);

        CompletableFuture.runAsync(() -> {
            try {
                GeeResponse response = performService(request);
                redisTemplate.opsForValue().set(TASK_CACHE_PREFIX + taskId, response);
                logger.info("Async task {} completed successfully", taskId);
            } catch (Exception e) {
                GeeResponse errorResponse = new GeeResponse("error", "Task failed: " + e.getMessage());
                redisTemplate.opsForValue().set(TASK_CACHE_PREFIX + taskId, errorResponse);
                logger.error("Async task {} failed", taskId, e);
            }
        });
        return taskId;
    }

    /**
     * Sends a POST request to the specified endpoint with the given request DTO.
     *
     * @param endpoint   the API endpoint to send the request to
     * @param requestDto the request data transfer object to be sent in the request
     *                   body
     * @param <T>        the type of the request DTO
     * @return the response from the API
     * @throws GeeProcessingException if there is an error during the request
     */
    @Retryable(value = { HttpClientErrorException.class }, maxAttempts = 3)
    private <T> GeeResponse sendPostRequest(String endpoint, T requestDto) {
        String url = flaskBaseUrl + endpoint;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(requestDto);
            logger.debug("Sending POST to {} with body: {}", url, requestBody);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize request body for {}", url, e);
            throw new GeeProcessingException("Failed to serialize request body", e);
        }

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
        try {
            ResponseEntity<GeeResponse> responseEntity = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, GeeResponse.class);
            logger.debug("Received response from {}: {}", url, responseEntity.getBody());
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                return responseEntity.getBody();
            } else {
                throw new GeeProcessingException(
                        "Request failed with status: " + responseEntity.getStatusCode().value());
            }
        } catch (HttpClientErrorException e) {
            handleHttpError(url, e);
            throw new GeeProcessingException(parseErrorResponse(e));
        } catch (Exception e) {
            logger.error("Unexpected error during POST to {}", url, e);
            throw new GeeProcessingException(new GeeResponse("error", "Unexpected error: " + e.getMessage()));
        }
    }

    /**
     * Sends a GET request to the specified endpoint.
     *
     * @param endpoint the API endpoint to send the request to
     * @return the response from the API
     * @throws GeeProcessingException if there is an error during the request
     */
    @Retryable(value = { HttpClientErrorException.class }, maxAttempts = 3)
    private GeeResponse sendGetRequest(String endpoint) {
        String url = flaskBaseUrl + endpoint;
        logger.debug("Sending GET to {}", url);
        try {
            ResponseEntity<GeeResponse> responseEntity = restTemplate.getForEntity(url, GeeResponse.class);
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                logger.debug("Received response from {}: {}", url, responseEntity.getBody());
                return responseEntity.getBody();
            } else {
                throw new GeeProcessingException(
                        "Request failed with status: " + responseEntity.getStatusCode().value());
            }
        } catch (HttpClientErrorException e) {
            handleHttpError(url, e);
            throw new GeeProcessingException(parseErrorResponse(e));
        } catch (Exception e) {
            logger.error("Unexpected error during GET to {}", url, e);
            throw new GeeProcessingException(new GeeResponse("error", "Unexpected error: " + e.getMessage()));
        }
    }

    /**
     * Performs a service request synchronously.
     *
     * @param requestDto the request data transfer object containing service details
     * @return the response from the service
     */
    public GeeResponse performService(GeeRequest requestDto) {
        validateGeeRequest(requestDto);
        String endpoint = "/api/" + requestDto.getServiceType();
        return sendPostRequest(endpoint, requestDto.getParameters());
    }

    /**
     * Retrieves the progress of a task by its ID.
     *
     * @param taskId the ID of the task
     * @return the progress of the task
     */
    public GeeResponse getTaskProgress(String taskId) {
        validateTaskId(taskId);
        GeeResponse cachedResponse = redisTemplate.opsForValue().get(TASK_CACHE_PREFIX + taskId);
        if (cachedResponse != null) {
            logger.info("Returning cached task progress for ID: {}", taskId);
            return cachedResponse;
        }
        return sendGetRequest("/api/task_progress/" + taskId);
    }

    /**
     * Saves the results of a GEE (Google Earth Engine) request.
     *
     * @param geeSaveRequest the request data transfer object containing the results
     *                       to save
     * @return the saved GEE results
     */
    @Transactional
    @CacheEvict(value = { "geeResultsById", "geeResultsByImageId", "allGeeResults" }, allEntries = true)
    public GeeResults save(GeeSaveRequest geeSaveRequest) {
        logger.info("Saving GeeResults for request: {}", geeSaveRequest);
        validateGeeSaveRequest(geeSaveRequest);

        Image image = null;
        if (geeSaveRequest.getImageId() != null) {
            image = imageRepository.findById(geeSaveRequest.getImageId())
                    .orElseThrow(() -> {
                        logger.error("Image not found with ID: {}", geeSaveRequest.getImageId());
                        return new ImageNotFoundException("Image not found with ID: " + geeSaveRequest.getImageId());
                    });
        }

        GeeResults geeResults = new GeeResults();
        geeResults.setData(geeSaveRequest.getData());
        geeResults.setDate(parseDate(geeSaveRequest.getDate()));
        geeResults.setType(geeSaveRequest.getType());
        geeResults.setImage(image);

        if (image != null) {
            image.getGeeResults().add(geeResults);
            imageRepository.save(image);
        }

        GeeResults savedResults = geeResultsRepository.save(geeResults);
        logger.info("GeeResults saved successfully with ID: {}", savedResults.getResultsId());
        return savedResults;
    }

    /**
     * Retrieves GEE results by their ID.
     *
     * @param id the ID of the GEE results
     * @return the GEE results
     */
    @Cacheable(value = "geeResultsById", key = "#id")
    public GeeResults getGeeResultsById(ObjectId id) {
        logger.info("Fetching GeeResults by ID: {}", id);
        validateObjectId(id, "GeeResults ID");
        return geeResultsRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("GeeResults not found with ID: {}", id);
                    return new IllegalArgumentException("GeeResults not found with ID: " + id);
                });
    }

    /**
     * Retrieves GEE results by the image ID.
     *
     * @param imageId the ID of the image
     * @return the list of GEE results associated with the image
     */
    @Cacheable(value = "geeResultsByImageId", key = "#imageId")
    public List<GeeResults> getGeeResultsByImageId(String imageId) {
        logger.info("Fetching GeeResults by image ID: {}", imageId);
        validateString(imageId, "Image ID");
        List<GeeResults> results = geeResultsRepository.findByImage_ImageId(imageId)
                .orElse(Collections.emptyList());
        if (results.isEmpty()) {
            logger.warn("No GeeResults found for image ID: {}", imageId);
        }
        return results;
    }

    /**
     * Retrieves all GEE results with pagination.
     *
     * @param pageable the pagination information
     * @return the page of GEE results
     */
    @Cacheable(value = "allGeeResults", key = "{#pageable.pageNumber, #pageable.pageSize, #pageable.sort}")
    public Page<GeeResults> getAllGeeResults(Pageable pageable) {
        logger.info("Fetching all GeeResults with pageable: {}", pageable);
        validatePageable(pageable);
        return geeResultsRepository.findAll(pageable);
    }

    /**
     * Deletes GEE results by their ID.
     *
     * @param id the ID of the GEE results to delete
     */
    @Transactional
    @CacheEvict(value = { "geeResultsById", "allGeeResults" }, key = "#id")
    public void deleteGeeResultsById(ObjectId id) {
        logger.info("Deleting GeeResults by ID: {}", id);
        validateObjectId(id, "GeeResults ID");
        if (!geeResultsRepository.existsById(id)) {
            logger.error("GeeResults not found with ID: {}", id);
            throw new IllegalArgumentException("GeeResults not found with ID: " + id);
        }
        geeResultsRepository.deleteById(id);
        logger.info("GeeResults deleted successfully with ID: {}", id);
    }

    /**
     * Deletes all GEE results associated with an image ID.
     *
     * @param imageId the ID of the image
     */
    @Transactional
    @CacheEvict(value = { "geeResultsByImageId", "allGeeResults" }, key = "#imageId")
    public void deleteGeeResultsByImageId(String imageId) {
        logger.info("Deleting all GeeResults for image ID: {}", imageId);
        validateString(imageId, "Image ID");
        if (!imageRepository.existsById(imageId)) {
            logger.error("Image not found with ID: {}", imageId);
            throw new ImageNotFoundException("Image not found with ID: " + imageId);
        }
        geeResultsRepository.deleteAllByImage_ImageId(imageId);
        logger.info("All GeeResults deleted successfully for image ID: {}", imageId);
    }

    /**
     * Deletes GEE results by image ID and result ID.
     *
     * @param imageId the ID of the image
     * @param id      the ID of the GEE results
     */
    public void deleteByImage_ImageIdAndId(String imageId, ObjectId id) {
        logger.info("Deleting GeeResults by image ID: {} and ID: {}", imageId, id);
        validateString(imageId, "Image ID");
        validateObjectId(id, "GeeResults ID");
        if (!imageRepository.existsById(imageId)) {
            logger.error("Image not found with ID: {}", imageId);
            throw new ImageNotFoundException("Image not found with ID: " + imageId);
        }
        geeResultsRepository.deleteByImage_ImageIdAndId(imageId, id);
        logger.info("GeeResults deleted successfully by image ID: {} and ID: {}", imageId, id);
    }

    /**
     * Updates a GEE results with the given ID and request body.
     *
     * @param id            the ID of the GEE results to update
     * @param updateRequest the request body containing the updated GEE results
     *                      information
     * @return the updated GEE results
     * @throws IllegalArgumentException if the GEE results is not found with the
     *                                  given ID
     * @throws ImageNotFoundException   if the image associated with the GEE results
     *                                  is not found with the given ID
     */
    @Transactional
    @CacheEvict(value = { "geeResultsById", "geeResultsByImageId", "allGeeResults" }, allEntries = true)
    public GeeResults updateGeeResults(ObjectId id, GeeSaveRequest updateRequest) {
        logger.info("Updating GeeResults with ID: {}", id);
        validateObjectId(id, "GeeResults ID");
        validateGeeSaveRequest(updateRequest);

        GeeResults existingResults = geeResultsRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("GeeResults not found with ID: {}", id);
                    return new IllegalArgumentException("GeeResults not found with ID: " + id);
                });

        Image image = null;
        if (updateRequest.getImageId() != null) {
            image = imageRepository.findById(updateRequest.getImageId())
                    .orElseThrow(() -> {
                        logger.error("Image not found with ID: {}", updateRequest.getImageId());
                        return new ImageNotFoundException("Image not found with ID: " + updateRequest.getImageId());
                    });
        }

        existingResults.setData(updateRequest.getData());
        existingResults.setDate(parseDate(updateRequest.getDate()));
        existingResults.setType(updateRequest.getType());
        existingResults.setImage(image);

        if (image != null) {
            image.getGeeResults().add(existingResults);
            imageRepository.save(image);
        }

        GeeResults updatedResults = geeResultsRepository.save(existingResults);
        logger.info("GeeResults updated successfully with ID: {}", updatedResults.getResultsId());
        return updatedResults;
    }

    /**
     * Cancels a task with the given ID. If the task is not found or already
     * completed, an IllegalStateException is thrown.
     * If the cancellation is successful, the task is removed from the cache. If the
     * cancellation fails, a GeeProcessingException
     * is thrown with the error message.
     *
     * @param taskId the ID of the task to cancel
     * @throws IllegalStateException  if the task is not found or already completed
     * @throws GeeProcessingException if the cancellation fails
     */
    public void cancelTask(String taskId) {
        logger.info("Attempting to cancel task with ID: {}", taskId);
        validateTaskId(taskId);

        GeeResponse cachedResponse = redisTemplate.opsForValue().get(TASK_CACHE_PREFIX + taskId);
        if (cachedResponse != null && !"completed".equalsIgnoreCase(cachedResponse.getStatus())) {
            GeeResponse cancelResponse = sendPostRequest("/api/cancel_task/" + taskId, null);
            if ("canceled".equalsIgnoreCase(cancelResponse.getStatus())) {
                redisTemplate.opsForValue().set(TASK_CACHE_PREFIX + taskId, cancelResponse);
                logger.info("Task {} canceled successfully", taskId);
            } else {
                logger.error("Failed to cancel task {}: {}", taskId, cancelResponse.getMessage());
                throw new GeeProcessingException("Failed to cancel task: " + cancelResponse.getMessage());
            }
        } else {
            logger.warn("Task {} is either completed or not found in cache", taskId);
            throw new IllegalStateException("Task is either completed or not found");
        }
    }

    /**
     * Bulk saves multiple GEE results in a single database transaction.
     *
     * @param geeSaveRequests the list of GEE results to save
     * @return the list of saved GEE results
     * @throws IllegalArgumentException if the input list is null or empty
     */
    @Transactional
    @CacheEvict(value = { "geeResultsById", "geeResultsByImageId", "allGeeResults" }, allEntries = true)
    public List<GeeResults> bulkSave(List<GeeSaveRequest> geeSaveRequests) {
        logger.info("Bulk saving {} GeeResults", geeSaveRequests.size());
        if (geeSaveRequests == null || geeSaveRequests.isEmpty()) {
            logger.error("Bulk save request list cannot be null or empty");
            throw new IllegalArgumentException("Bulk save request list cannot be null or empty");
        }

        List<GeeResults> resultsList = new ArrayList<>();
        for (GeeSaveRequest request : geeSaveRequests) {
            GeeResults savedResult = save(request); // Reuse existing save method
            resultsList.add(savedResult);
        }
        logger.info("Bulk save completed successfully with {} results", resultsList.size());
        return resultsList;
    }

    /**
     * Retrieves a list of tasks with the given status from the cache (Redis in
     * this implementation). The list is limited to the given number of elements.
     * 
     * @param status the status of tasks to retrieve (e.g. "failed", "completed")
     * @param limit  the maximum number of tasks to retrieve
     * @return a list of tasks with the given status, limited to the given number
     *         of elements
     * @throws IllegalArgumentException if the status is null or empty, or if the
     *                                  limit is not positive
     */
    public List<GeeResponse> getTasksByStatus(String status, int limit) {
        logger.info("Fetching tasks with status: {} (limit: {})", status, limit);
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }

        List<GeeResponse> tasks = new ArrayList<>();
        // This assumes Redis stores all tasks; in practice, you might need a different
        // approach
        // Here, we simulate filtering from Redis (not ideal, but works with current
        // setup)
        List<String> keys = redisTemplate.keys(TASK_CACHE_PREFIX + "*").stream()
                .limit(limit)
                .toList();
        for (String key : keys) {
            GeeResponse response = redisTemplate.opsForValue().get(key);
            if (response != null && status.equalsIgnoreCase(response.getStatus())) {
                tasks.add(response);
            }
        }
        logger.info("Found {} tasks with status: {}", tasks.size(), status);
        return tasks;
    }


    /**
     * Asynchronously retries a failed task with the given ID. The task ID must
     * exist in the cache (Redis in this implementation) and have a status of
     * "error". The original request is assumed to be stored separately and is
     * reused to retry the task. If the task is not found or not in a failed state,
     * an exception is thrown.
     * 
     * @param taskId the ID of the task to retry
     * @return the ID of the newly created task
     * @throws IllegalStateException if the task is not found or not in a failed
     *                               state
     */
    @Async("asyncTaskExecutor")
    public String retryFailedTask(String taskId) {
        logger.info("Retrying failed task with ID: {}", taskId);
        validateTaskId(taskId);

        GeeResponse cachedResponse = redisTemplate.opsForValue().get(TASK_CACHE_PREFIX + taskId);
        if (cachedResponse == null || !"error".equalsIgnoreCase(cachedResponse.getStatus())) {
            logger.warn("Task {} cannot be retried: not found or not failed", taskId);
            throw new IllegalStateException("Task not found or not in failed state");
        }

        // Retrieve the original request from Redis
        GeeRequest originalRequest = redisRequestTemplate.opsForValue().get(REQUEST_CACHE_PREFIX + taskId);
        if (originalRequest == null) {
            logger.error("Original request for task {} not found in cache", taskId);
            throw new IllegalStateException("Original request not found for task: " + taskId);
        }

        // Start a new task with the original request
        String newTaskId = startAsyncTask(originalRequest);
        logger.info("Retry initiated for task {} as new task {}", taskId, newTaskId);
        return newTaskId;
    }

    // Validation Helpers

    /**
     * Validates a GEE request.
     *
     * @param request the GEE request to validate
     * @throws IllegalArgumentException if the request is invalid
     */
    private void validateGeeRequest(GeeRequest request) {
        if (request == null || request.getServiceType() == null || request.getParameters() == null) {
            logger.error("Invalid GeeRequest: {}", request);
            throw new IllegalArgumentException("GeeRequest, serviceType, and parameters cannot be null");
        }
    }

    /**
     * Validates a GEE save request.
     *
     * @param request the GEE save request to validate
     * @throws IllegalArgumentException if the request is invalid
     */
    private void validateGeeSaveRequest(GeeSaveRequest request) {
        if (request == null || request.getData() == null || request.getDate() == null || request.getType() == null) {
            logger.error("Invalid GeeSaveRequest: {}", request);
            throw new IllegalArgumentException("GeeSaveRequest, data, date, and type cannot be null");
        }
    }

    /**
     * Validates a task ID.
     *
     * @param taskId the task ID to validate
     * @throws IllegalArgumentException if the task ID is invalid
     */
    private void validateTaskId(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            logger.error("Task ID cannot be null or empty");
            throw new IllegalArgumentException("Task ID cannot be null or empty");
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
     * Validates a Pageable object.
     *
     * @param pageable the Pageable object to validate
     * @throws IllegalArgumentException if the Pageable object is invalid
     */
    private void validatePageable(Pageable pageable) {
        if (pageable == null || pageable.getPageNumber() < 0 || pageable.getPageSize() <= 0) {
            logger.error("Invalid pageable: {}", pageable);
            throw new IllegalArgumentException("Pageable must be valid with non-negative page and positive size");
        }
    }

    /**
     * Parses a date string into a LocalDateTime object.
     *
     * @param dateStr the date string to parse
     * @return the parsed LocalDateTime object
     * @throws IllegalArgumentException if the date string is invalid
     */
    private LocalDateTime parseDate(String dateStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
            return LocalDateTime.parse(dateStr, formatter);
        } catch (Exception e) {
            logger.error("Invalid date format: {}", dateStr, e);
            throw new IllegalArgumentException("Invalid date format: " + dateStr, e);
        }
    }

    /**
     * Handles HTTP errors by logging the error details.
     *
     * @param url the URL that caused the error
     * @param e   the HTTP client error exception
     */
    private void handleHttpError(String url, HttpClientErrorException e) {
        logger.error("HTTP error for {}: Status: {}, Body: {}", url, e.getStatusCode(), e.getResponseBodyAsString(), e);
    }

    /**
     * Parses an HTTP error response into a GeeResponse object.
     *
     * @param e the HTTP client error exception
     * @return the GeeResponse object representing the error
     */
    private GeeResponse parseErrorResponse(HttpClientErrorException e) {
        try {
            return objectMapper.readValue(e.getResponseBodyAsString(), GeeResponse.class);
        } catch (JsonProcessingException ex) {
            logger.error("Failed to parse error response from Flask backend: {}", e.getResponseBodyAsString(), ex);
            return new GeeResponse("error", "Failed to parse error response from Flask backend");
        }
    }
}
