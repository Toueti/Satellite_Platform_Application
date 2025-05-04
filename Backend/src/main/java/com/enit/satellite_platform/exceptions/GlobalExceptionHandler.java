package com.enit.satellite_platform.exceptions;

import com.enit.satellite_platform.modules.project_management.exceptions.ProjectAlreadyExistsException;
import com.enit.satellite_platform.modules.project_management.exceptions.ProjectNameNotFoundException;
import com.enit.satellite_platform.modules.project_management.exceptions.ProjectNotFoundException;
import com.enit.satellite_platform.modules.project_management.exceptions.ProjectValidationException;
import com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.gee.exception.EarthEngineConfigException;
import com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.gee.exception.GeeProcessingException;
import com.enit.satellite_platform.modules.resource_management.image_management.exceptions.ImageDownloadException;
import com.enit.satellite_platform.modules.resource_management.image_management.exceptions.ImageException;
import com.enit.satellite_platform.modules.resource_management.image_management.exceptions.ImageMetadataException;
import com.enit.satellite_platform.modules.resource_management.image_management.exceptions.ImageNotFoundException;
import com.enit.satellite_platform.modules.resource_management.image_management.exceptions.ImageProcessingException;
import com.enit.satellite_platform.modules.resource_management.image_management.exceptions.ImageStorageException;
import com.enit.satellite_platform.modules.resource_management.image_management.exceptions.ImageValidationException;
import com.enit.satellite_platform.modules.resource_management.image_management.exceptions.InvalidImageDataException;
import com.enit.satellite_platform.modules.resource_management.image_management.exceptions.ThumbnailGenerationException;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.exceptions.InvalidCredentialsException;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.exceptions.RoleNotFoundException;
import com.enit.satellite_platform.shared.dto.ApiErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Global exception handler for the Satellite Platform application. This class
 * handles various exceptions and returns appropriate HTTP responses.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * Handles ProjectAlreadyExistsException.
	 *
	 * @param ex the ProjectAlreadyExistsException
	 * @return a ResponseEntity with the exception message and HTTP status CONFLICT
	 */
	@ExceptionHandler(ProjectAlreadyExistsException.class)
	public ResponseEntity<String> handleProjectAlreadyExistsException(ProjectAlreadyExistsException ex) {
		return new ResponseEntity<>(ex.getMessage(), HttpStatus.CONFLICT);
	}

	/**
	 * Handles ProjectNameNotFoundException.
	 *
	 * @param ex the ProjectNameNotFoundException
	 * @return a ResponseEntity with the exception message and HTTP status NOT_FOUND
	 */
	@ExceptionHandler(ProjectNameNotFoundException.class)
	public ResponseEntity<String> handleProjectNameNotFoundException(ProjectNameNotFoundException ex) {
		return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
	}

	/**
	 * Handles ProjectNotFoundException.
	 *
	 * @param ex the ProjectNotFoundException
	 * @return a ResponseEntity with the exception message and HTTP status NOT_FOUND
	 */
	@ExceptionHandler(ProjectNotFoundException.class)
	public ResponseEntity<String> handleProjectNotFoundException(ProjectNotFoundException ex) {
		return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
	}

	/**
	 * Handles ProjectValidationException.
	 *
	 * @param ex the ProjectValidationException
	 * @return a ResponseEntity with the exception message and HTTP status BAD_REQUEST
	 */
	@ExceptionHandler(ProjectValidationException.class)
	public ResponseEntity<String> handleProjectValidationException(ProjectValidationException ex) {
		return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
	}

	/**
	 * Handles InvalidImageDataException.
	 *
	 * @param ex the InvalidImageDataException
	 * @return a ResponseEntity with the exception message and HTTP status BAD_REQUEST
	 */
	@ExceptionHandler(InvalidImageDataException.class)
	public ResponseEntity<String> handleInvalidImageDataException(InvalidImageDataException ex) {
		return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
	}

	/**
	 * Handles ImageProcessingException.
	 *
	 * @param ex the ImageProcessingException
	 * @return a ResponseEntity with the exception message and HTTP status
	 *         INTERNAL_SERVER_ERROR
	 */
	@ExceptionHandler(ImageProcessingException.class)
	public ResponseEntity<String> handleImageProcessingException(ImageProcessingException ex) {
		return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/**
	 * Handles DataIntegrityViolationException.
	 *
	 * @param ex the DataIntegrityViolationException
	 * @return a ResponseEntity with a custom message and HTTP status BAD_REQUEST
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<String> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
		// Customize this message based on common causes, like unique constraint
		// violations
		return new ResponseEntity<>(
				"Data integrity violation: " + ex.getMessage()
						+ ". Please ensure your input is valid and does not conflict with existing data.",
				HttpStatus.BAD_REQUEST);
	}

	/**
	 * Handles AccessDeniedException.
	 *
	 * @param ex the AccessDeniedException
	 * @return a ResponseEntity with a custom message and HTTP status FORBIDDEN
	 */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<String> handleAccessDeniedException(AccessDeniedException ex) {
		return new ResponseEntity<>(
				"Access denied: " + ex.getMessage() + ". You may not have the necessary permissions.",
				HttpStatus.FORBIDDEN);
	}

	/**
	 * Handles UsernameNotFoundException.
	 *
	 * @param ex the UsernameNotFoundException
	 * @return a ResponseEntity with a custom message and HTTP status NOT_FOUND
	 */
	@ExceptionHandler(UsernameNotFoundException.class)
	public ResponseEntity<String> handleUsernameNotFoundException(UsernameNotFoundException ex) {
		return new ResponseEntity<>("User not found: " + ex.getMessage() + ". Please check your credentials.",
				HttpStatus.NOT_FOUND);
	}

	/**
	 * Handles EarthEngineConfigException.
	 *
	 * @param ex the EarthEngineConfigException
	 * @return a ResponseEntity with the exception message and HTTP status
	 *         INTERNAL_SERVER_ERROR
	 */
	@ExceptionHandler(EarthEngineConfigException.class)
	public ResponseEntity<String> handleEarthEngineConfigException(EarthEngineConfigException ex) {
		return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/**
	 * Handles ImageDownloadException.
	 *
	 * @param ex the ImageDownloadException
	 * @return a ResponseEntity with the exception message and HTTP status
	 *         INTERNAL_SERVER_ERROR
	 */
	@ExceptionHandler(ImageDownloadException.class)
	public ResponseEntity<String> handleImageDownloadException(ImageDownloadException ex) {
		return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/**
	 * Handles ImageException.
	 *
	 * @param ex the ImageException
	 * @return a ResponseEntity with the exception message and HTTP status
	 *         INTERNAL_SERVER_ERROR
	 */
	@ExceptionHandler(ImageException.class)
	public ResponseEntity<String> handleImageException(ImageException ex) {
		return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/**
	 * Handles ImageMetadataException.
	 *
	 * @param ex the ImageMetadataException
	 * @return a ResponseEntity with the exception message and HTTP status
	 *         INTERNAL_SERVER_ERROR
	 */
	@ExceptionHandler(ImageMetadataException.class)
	public ResponseEntity<String> handleImageMetadataException(ImageMetadataException ex) {
		return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/**
	 * Handles ImageStorageException.
	 *
	 * @param ex the ImageStorageException
	 * @return a ResponseEntity with the exception message and HTTP status
	 *         INTERNAL_SERVER_ERROR
	 */
	@ExceptionHandler(ImageStorageException.class)
	public ResponseEntity<String> handleImageStorageException(ImageStorageException ex) {
		return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/**
	 * Handles ImageValidationException.
	 *
	 * @param ex the ImageValidationException
	 * @return a ResponseEntity with the exception message and HTTP status BAD_REQUEST
	 */
	@ExceptionHandler(ImageValidationException.class)
	public ResponseEntity<String> handleImageValidationException(ImageValidationException ex) {
		return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
	}

	/**
	 * Handles ThumbnailGenerationException.
	 *
	 * @param ex the ThumbnailGenerationException
	 * @return a ResponseEntity with the exception message and HTTP status
	 *         INTERNAL_SERVER_ERROR
	 */
	@ExceptionHandler(ThumbnailGenerationException.class)
	public ResponseEntity<String> handleThumbnailGenerationException(ThumbnailGenerationException ex) {
		return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/**
	 * Handles InvalidCredentialsException.
	 *
	 * @param ex the InvalidCredentialsException
	 * @return a ResponseEntity with the exception message and HTTP status
	 *         UNAUTHORIZED
	 */
	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<String> handleInvalidCredentialsException(InvalidCredentialsException ex) {
		return new ResponseEntity<>(ex.getMessage(), HttpStatus.UNAUTHORIZED);
	}

	/**
	 * Handles RoleNotFoundException.
	 *
	 * @param ex the RoleNotFoundException
	 * @return a ResponseEntity with the exception message and HTTP status BAD_REQUEST
	 */
	@ExceptionHandler(RoleNotFoundException.class)
	public ResponseEntity<String> handleRoleNotFoundException(RoleNotFoundException ex) {
		return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
	}

	/**
	 * Handles MethodArgumentNotValidException.
	 *
	 * @param ex the MethodArgumentNotValidException
	 * @return a ResponseEntity with validation errors and HTTP status BAD_REQUEST
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
		String errors = ex.getBindingResult().getFieldErrors().stream()
				.map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
				.collect(Collectors.joining(", "));
		return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
	}

	/**
	 * Handles GeeProcessingException.
	 *
	 * @param ex the GeeProcessingException
	 * @return a ResponseEntity with an ApiErrorResponse and HTTP status
	 *         INTERNAL_SERVER_ERROR
	 */
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

	/**
	 * Handles ImageNotFoundException.
	 *
	 * @param ex the ImageNotFoundException
	 * @return a ResponseEntity with an ApiErrorResponse and HTTP status NOT_FOUND
	 */
	@ExceptionHandler(ImageNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleImageNotFound(ImageNotFoundException ex) {
		ApiErrorResponse error = new ApiErrorResponse("IMAGE_NOT_FOUND", ex.getMessage());
		error.setStatus(HttpStatus.NOT_FOUND.value());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	/**
	 * Handles generic exceptions.
	 *
	 * @param ex      the Exception
	 * @param request the WebRequest
	 * @return a ResponseEntity with a generic error message and HTTP status
	 *         INTERNAL_SERVER_ERROR
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<Object> handleGenericException(Exception ex, WebRequest request) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", LocalDateTime.now());
		body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
		body.put("error", "Internal Server Error");
		body.put("message", ex.getMessage());
		return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
