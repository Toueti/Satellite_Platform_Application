package com.enit.satellite_platform.resources_management.controllers;

import com.enit.satellite_platform.resources_management.dto.GeeRequest;
import com.enit.satellite_platform.resources_management.dto.GeeResponse;
import com.enit.satellite_platform.resources_management.services.GeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/thematician/tasks")
@Tag(name = "Task Controller", description = "Endpoints for managing asynchronous GEE tasks")
public class TaskController {

    @Autowired
    private GeeService geeService;

    /**
     * Cancels an ongoing asynchronous GEE task by its ID.
     *
     * @param taskId the ID of the task to cancel
     * @return a ResponseEntity with a 204 status code if the task was canceled successfully
     * @throws GeeProcessingException if the task is not found or if there is an error during cancellation
     */
    @DeleteMapping("/task/{taskId}")
    @Operation(summary = "Cancel an asynchronous task", description = "Cancels an ongoing asynchronous GEE task by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Task canceled successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "404", description = "Task not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> cancelTask(
            @Parameter(description = "Task ID", required = true) @PathVariable String taskId) {
        geeService.cancelTask(taskId);
        return ResponseEntity.noContent().build();
    }

    
    /**
     * Retrieves a list of tasks filtered by their status.
     * 
     * @param status the status of tasks to retrieve (e.g. "running", "completed", "error")
     * @param limit  the maximum number of tasks to retrieve
     * @return a list of tasks with the given status, limited to the given number of elements
     * @throws IllegalArgumentException if the status is null or empty, or if the limit is not positive
     */
    @GetMapping("/tasks/status/{status}")
    @Operation(summary = "Get tasks by status", description = "Retrieves a list of tasks filtered by their status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<GeeResponse>> getTasksByStatus(
            @Parameter(description = "Task status (e.g., running, completed, error)", required = true) @PathVariable String status,
            @Parameter(description = "Maximum number of tasks to return", required = false) @RequestParam(defaultValue = "10") int limit) {
        List<GeeResponse> tasks = geeService.getTasksByStatus(status, limit);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Retries a previously failed asynchronous GEE task and returns a new task ID.
     * 
     * @param taskId the ID of the failed task
     * @return a ResponseEntity with a 202 status code if the task retry was successful, containing the new task ID
     * @throws GeeProcessingException if the task is not found or if there is an error during retry
     */
    @PostMapping("/task/retry/{taskId}")
    @Operation(summary = "Retry a failed task", description = "Retries a previously failed asynchronous GEE task and returns a new task ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Task retry started successfully, new task ID returned"),
            @ApiResponse(responseCode = "400", description = "Bad request or task not in error state"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> retryFailedTask(
            @Parameter(description = "Task ID of the failed task", required = true) @PathVariable String taskId) {
        try {
            String newTaskId = geeService.retryFailedTask(taskId);
            return ResponseEntity.accepted().body(newTaskId);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retry task: " + e.getMessage());
        }
    }

/**
 * Starts a new asynchronous task using the provided GEE request details.
 *
 * @param request the GEE request details required to initiate the task
 * @return a ResponseEntity containing the task ID if the task is started successfully,
 *         or an error message if the task initiation fails
 * @throws IllegalArgumentException if the request is invalid
 * @throws Exception if there is an internal server error during task initiation
 */

    @Operation(summary = "Start a new asynchronous task", description = "Initiates an asynchronous GEE task and returns the task ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Task started successfully, returns task ID"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/start")
    public ResponseEntity<String> startTask(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "GEE request details", required = true) @Valid @RequestBody GeeRequest request) {
        try {
            String taskId = geeService.startAsyncTask(request);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(taskId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to start task: " + e.getMessage());
        }
    }

    /**
     * Retrieves the current status of a task by its ID.
     *
     * @param taskId the ID of the task
     * @return a ResponseEntity containing the task status if the task is found,
     *         or an error message if the task is not found or if there is an
     *         internal server error
     * @throws IllegalArgumentException if the task ID is invalid
     * @throws Exception if there is an internal server error during task status
     *                   retrieval
     */
    @Operation(summary = "Get the status of an asynchronous task", description = "Retrieves the current status of a task by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task status retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid task ID"),
            @ApiResponse(responseCode = "404", description = "Task not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/status/{taskId}")
    public ResponseEntity<GeeResponse> getTaskStatus(
            @Parameter(description = "Task ID", required = true) @PathVariable @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Task ID must contain only alphanumeric characters and hyphens") String taskId) {
        try {
            GeeResponse response = geeService.getTaskProgress(taskId);
            if (response == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new GeeResponse("error", "Invalid task ID: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GeeResponse("error", "Failed to retrieve task status: " + e.getMessage()));
        }
    }

    /**
     * Starts an asynchronous task for the given GEE request. The task is executed
     * in a separate thread and the task ID is returned immediately. The task ID can
     * be used to retrieve the task status using the getTaskStatus method.
     *
     * @param requestDto the GEE request to process
     * @return the ID of the started task if the task is started successfully,
     *         or an error message if the task cannot be started
     * @throws GeeProcessingException if there is an error during the request
     */
    @PostMapping("/async-service")
    @Operation(summary = "Start an asynchronous GEE task", description = "Starts an asynchronous task for the provided GEE request and returns the task ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Task started successfully, task ID returned"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> startAsyncTask(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "GEE request details for the async task", required = true) @Valid @RequestBody GeeRequest requestDto) {
        String taskId = geeService.startAsyncTask(requestDto);
        return ResponseEntity.accepted().body(taskId);
    }

    /**
     * Retrieves the progress of a specific task.
     *
     * @param taskId the ID of the task to retrieve progress for
     * @return the task progress if the task is found and its progress can be retrieved
     * @throws GeeProcessingException if there is an error during the request
     */
    @GetMapping("/task-progress/{taskId}")
    @Operation(summary = "Get task progress", description = "Retrieves the progress of a specific task")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task progress retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Task not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<GeeResponse> getTaskProgress(
            @Parameter(description = "Task ID", required = true) @PathVariable String taskId) {
        GeeResponse response = geeService.getTaskProgress(taskId);
        return ResponseEntity.ok(response);
    }
}