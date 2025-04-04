package com.enit.satellite_platform.modules.project_management.config;

import com.enit.satellite_platform.modules.project_management.exceptions.ProjectAlreadyExistsException;
import com.enit.satellite_platform.modules.project_management.exceptions.ProjectNameNotFoundException;
import com.enit.satellite_platform.modules.project_management.exceptions.ProjectNotFoundException;
import com.enit.satellite_platform.modules.project_management.exceptions.ProjectValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ProjectExceptionHandler {

    @ExceptionHandler(ProjectAlreadyExistsException.class)
    public ResponseEntity<String> handleProjectAlreadyExistsException(ProjectAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(ProjectNameNotFoundException.class)
    public ResponseEntity<String> handleProjectNameNotFoundException(ProjectNameNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<String> handleProjectNotFoundException(ProjectNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
    @ExceptionHandler(ProjectValidationException.class)
    public ResponseEntity<String> handleProjectValidationException(ProjectValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
