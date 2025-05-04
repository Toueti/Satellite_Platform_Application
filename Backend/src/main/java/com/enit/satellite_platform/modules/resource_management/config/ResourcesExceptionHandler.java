package com.enit.satellite_platform.modules.resource_management.config;

import com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.gee.exception.EarthEngineConfigException;
import com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.gee.exception.GeeProcessingException;
import com.enit.satellite_platform.modules.resource_management.image_management.exceptions.*;
import com.enit.satellite_platform.shared.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ResourcesExceptionHandler {

    @ExceptionHandler(EarthEngineConfigException.class)
    public ResponseEntity<String> handleEarthEngineConfigException(EarthEngineConfigException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(GeeProcessingException.class)
    public ResponseEntity<ApiErrorResponse> handleGeeError(GeeProcessingException ex) {
        ApiErrorResponse error;
        if (ex.getGeeResponse() != null && ex.getGeeResponse().getMessage() != null) {
            error = new ApiErrorResponse("GEE_ERROR", ex.getGeeResponse().getMessage());
            error.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        } else {
            error = new ApiErrorResponse("GEE_ERROR", ex.getMessage());
            error.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    @ExceptionHandler(ImageDownloadException.class)
    public ResponseEntity<String> handleImageDownloadException(ImageDownloadException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ImageException.class)
    public ResponseEntity<String> handleImageException(ImageException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ImageMetadataException.class)
    public ResponseEntity<String> handleImageMetadataException(ImageMetadataException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ImageNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleImageNotFound(ImageNotFoundException ex) {
        ApiErrorResponse error = new ApiErrorResponse("IMAGE_NOT_FOUND", ex.getMessage());
        error.setStatus(HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    @ExceptionHandler(ImageProcessingException.class)
    public ResponseEntity<String> handleImageProcessingException(ImageProcessingException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    @ExceptionHandler(ImageStorageException.class)
    public ResponseEntity<String> handleImageStorageException(ImageStorageException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    @ExceptionHandler(ImageValidationException.class)
    public ResponseEntity<String> handleImageValidationException(ImageValidationException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(InvalidImageDataException.class)
    public ResponseEntity<String> handleInvalidImageDataException(InvalidImageDataException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(ThumbnailGenerationException.class)
    public ResponseEntity<String> handleThumbnailGenerationException(ThumbnailGenerationException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
