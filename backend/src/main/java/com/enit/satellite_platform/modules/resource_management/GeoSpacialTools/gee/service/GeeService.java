package com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.gee.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.retry.annotation.Retryable;

import org.springframework.stereotype.Service;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.enit.satellite_platform.config.cache_handler.CacheKeyGenerator;
import com.enit.satellite_platform.config.cache_handler.SatelliteProcessingCacheHandler;
import com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.gee.exception.GeeProcessingException;
import com.enit.satellite_platform.modules.resource_management.dto.ProcessingResponse;
import com.enit.satellite_platform.modules.resource_management.dto.ServiceRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

/**
 * Service class for handling operations related to Google Earth Engine (GEE)
 * tasks.
 * Uses SatelliteProcessingCacheHandler for caching GEE processing responses.
 */
@Service
@RefreshScope
public class GeeService {

    private static final Logger logger = LoggerFactory.getLogger(GeeService.class);

    @Autowired
    private RestTemplate restTemplate;

    // Removed RedisTemplate injections

    @Value("${python.backend.url}")
    private String flaskBaseUrl;

    // Removed ResourceCacheHandler injection

    @Autowired
    private SatelliteProcessingCacheHandler geeResponseCacheHandler;

    @Autowired // Added CacheKeyGenerator injection
    private CacheKeyGenerator cacheKeyGenerator;

    @Autowired
    private ObjectMapper objectMapper;


    /**
     * Process a GEE request by sending it to the Flask backend, using
     * GeeResponseCacheHandler for caching.
     *
     * @param request the GEE request
     * @return the GEE response
     */
    public ProcessingResponse processGeeRequest(ServiceRequest request) {
        validateGeeRequest(request);
        // Generate cache key using CacheKeyGenerator
        String cacheKey = cacheKeyGenerator.generateKey(request);

        // Use GeeResponseCacheHandler to get data using the generated key
        Optional<ProcessingResponse> cachedResponse = geeResponseCacheHandler.getResourceData(cacheKey);

        if (cachedResponse.isPresent()) {
            logger.info("Returning cached GeeResponse for key: {}", cacheKey);
            return cachedResponse.get();
        }

        // Process request if not found in cache
        logger.info("Processing GEE request (cache miss for key: {})", cacheKey);
        String endpoint = flaskBaseUrl + request.getServiceType();
        ProcessingResponse response = sendPostRequest(endpoint, request.getParameters());

        // Use GeeResponseCacheHandler to cache the new response using the generated key
        // The CacheHandler manages TTL and access counts internally
        geeResponseCacheHandler.storeResourceData(response, cacheKey);
        logger.info("Cached new GeeResponse for key: {}", cacheKey);


        return response;
    }

    /**
     * Sends a POST request to the specified endpoint with the given request
     * body.
     *
     * <p>
     * This method will retry the request up to three times if it fails due to a
     * timeout or an exception. If it still fails after three retries, it will
     * throw a {@link GeeProcessingException}.
     *
     * @param endpoint   the URL of the endpoint to POST to
     * @param requestDto the request body to serialize and send
     * @return the response from the server
     * @throws GeeProcessingException if the request fails after three retries
     */
    @Retryable(value = { HttpClientErrorException.class }, maxAttempts = 3)
    private <T> ProcessingResponse sendPostRequest(String endpoint, T requestDto) {
        String url = endpoint;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(requestDto);
            logger.debug("Sending POST to {} with body: {}", url, requestBody);
        } catch (JsonProcessingException e) {
            throw new GeeProcessingException("Failed to serialize request body", e);
        }

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
        try {
            ResponseEntity<ProcessingResponse> responseEntity = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, ProcessingResponse.class);
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
            throw new GeeProcessingException(new ProcessingResponse("error", "Unexpected error: " + e.getMessage()));
        }
    }

    /**
     * Sends a GET request to the specified endpoint.
     *
     * <p>
     * This method will retry the request up to three times if it fails due to a
     * {@link HttpClientErrorException}. If it still fails after three retries, it
     * will throw a {@link GeeProcessingException}.
     *
     * @param endpoint the relative endpoint URL to send the GET request to
     * @return the response from the server as a {@link ProcessingResponse}
     * @throws GeeProcessingException if the request fails after three retries or
     *                                if an unexpected error occurs
     */
    @Retryable(value = { HttpClientErrorException.class }, maxAttempts = 3)
    private ProcessingResponse sendGetRequest(String endpoint) {
        String url = flaskBaseUrl + endpoint;
        logger.debug("Sending GET to {}", url);
        try {
            ResponseEntity<ProcessingResponse> responseEntity = restTemplate.getForEntity(url,
                    ProcessingResponse.class);
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
            throw new GeeProcessingException(new ProcessingResponse("error", "Unexpected error: " + e.getMessage()));
        }
    }

    // --- Validation and Helper Methods ---

    private void validateGeeRequest(ServiceRequest request) {
        if (request == null || request.getServiceType() == null || request.getParameters() == null) {
            logger.error("Invalid GeeRequest: {}", request);
            throw new IllegalArgumentException("GeeRequest, serviceType, and parameters cannot be null");
        }
    }

    private void handleHttpError(String url, HttpClientErrorException e) {
        logger.error("HTTP error for {}: Status: {}, Body: {}", url, e.getStatusCode(), e.getResponseBodyAsString(), e);
    }

    private ProcessingResponse parseErrorResponse(HttpClientErrorException e) {
        try {
            return objectMapper.readValue(e.getResponseBodyAsString(), ProcessingResponse.class);
        } catch (JsonProcessingException ex) {
            logger.error("Failed to parse error response from Flask backend: {}", e.getResponseBodyAsString(), ex);
            return new ProcessingResponse("error", "Failed to parse error response from Flask backend");
        }
    }

}
