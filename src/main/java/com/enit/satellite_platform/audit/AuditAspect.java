package com.enit.satellite_platform.audit;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuditAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger("AuditLogger");

    // Pointcut for login attempts
    @Pointcut("execution(* com.enit.satellite_platform.user_management.controller.AuthController.login(..))")
    public void loginAttempt() {}

    // Pointcut for project creation
    @Pointcut("execution(* com.enit.satellite_platform.project_management.service.ProjectService.createProject(..))")
    public void projectCreation() {}

    // Pointcut for project access (adjust as needed based on methods in ProjectService)
    @Pointcut("execution(* com.enit.satellite_platform.project_management.service.ProjectService.getProjectById(..))")
    public void projectAccess() {}
    
     // Pointcut for project sharing
    @Pointcut("execution(* com.enit.satellite_platform.project_management.service.ProjectService.shareProject(..))")
    public void projectSharing() {}

    // Pointcut for user updates
    @Pointcut("execution(* com.enit.satellite_platform.user_management.service.UserService.updateUser(..))")
    public void userUpdate() {}

    @Before("loginAttempt()")
    public void logLoginAttempt(JoinPoint joinPoint) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String username = "Unknown"; // Default value
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] != null) {
            // Assuming the first argument is a LoginRequest DTO or similar
            try {
                // Use reflection or direct access if the structure is known
                java.lang.reflect.Method method = args[0].getClass().getMethod("getUsername");
                username = (String) method.invoke(args[0]);
            } catch (Exception e) {
                // Handle reflection exceptions
                LOGGER.warn("Could not extract username from login request", e);
            }
        }
        LOGGER.info("Login attempt for user: {} from IP: {}", username, request.getRemoteAddr());
    }

    @AfterReturning(pointcut = "loginAttempt()", returning = "result")
    public void logLoginSuccess(JoinPoint joinPoint, Object result) {
        // Assuming a successful login returns a JwtResponse or similar with a username
        String username = "Unknown";
        if (result != null) {
            try {
                java.lang.reflect.Method method = result.getClass().getMethod("getUsername");
                username = (String) method.invoke(result);
            } catch (Exception e) {
                // Try another common field, like 'userId'
                try {
                    java.lang.reflect.Method userIdMethod = result.getClass().getMethod("getUserId");
                    username = (String) userIdMethod.invoke(result);
                }
                catch(Exception ex) {
                    LOGGER.warn("Could not extract username from login response", e);
                }
            }
        }
        LOGGER.info("Successful login for user: {}", username);
    }

    @AfterThrowing(pointcut = "loginAttempt()", throwing = "error")
    public void logLoginFailure(JoinPoint joinPoint, Throwable error) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String username = "Unknown"; // Default value
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] != null) {
            try {
                java.lang.reflect.Method method = args[0].getClass().getMethod("getUsername");
                username = (String) method.invoke(args[0]);
            } catch (Exception e) {
                LOGGER.warn("Could not extract username from login request", e);
            }
        }
        LOGGER.error("Failed login attempt for user: {} from IP: {}. Error: {}", username, request.getRemoteAddr(), error.getMessage());
    }

    @Before("projectCreation()")
    public void logProjectCreation(JoinPoint joinPoint) {
        String username = getCurrentUsername();
        Object[] args = joinPoint.getArgs();
        String projectName = "Unknown";
        if(args.length > 0 && args[0] != null) {
            try {
                java.lang.reflect.Method method = args[0].getClass().getMethod("getName");
                projectName = (String) method.invoke(args[0]);

            } catch(Exception e) {
                LOGGER.warn("Could not extract project name from project creation request", e);
            }
        }
        LOGGER.info("Project creation attempt: Project Name {}, User: {}", projectName, username);
    }

    @AfterReturning(pointcut = "projectCreation()", returning = "result")
    public void logProjectCreationResult(JoinPoint joinPoint, Object result) {
        String username = getCurrentUsername();
        Long projectId = null;
         if (result != null) {
            try {
                java.lang.reflect.Method method = result.getClass().getMethod("getId");
                projectId = (Long) method.invoke(result);
            } catch (Exception e) {
                LOGGER.warn("Could not extract project id from project creation response", e);
            }
        }
        LOGGER.info("Project created: Project ID {}, User: {}", projectId, username);
    }

    @AfterThrowing(pointcut = "projectCreation()", throwing = "error")
    public void logProjectCreationFailure(JoinPoint joinPoint, Throwable error) {
        String username = getCurrentUsername();
        Object[] args = joinPoint.getArgs();
        String projectName = "Unknown";
        if(args.length > 0 && args[0] != null) {
            try {
                java.lang.reflect.Method method = args[0].getClass().getMethod("getName");
                projectName = (String) method.invoke(args[0]);

            } catch(Exception e) {
                LOGGER.warn("Could not extract project name from project creation request", e);
            }
        }
        LOGGER.error("Project creation failed: Project Name {}, User: {}, Error: {}", projectName, username, error.getMessage());
    }

    @Before("projectAccess()")
    public void logProjectAccess(JoinPoint joinPoint) {
        String username = getCurrentUsername();
        Object[] args = joinPoint.getArgs();
        Long projectId = (Long) args[0]; // Assuming the first argument is the project ID

        LOGGER.info("Project access attempt: Project ID: {}, User: {}", projectId, username);
    }
    
    @AfterReturning(pointcut = "projectAccess()", returning = "result")
    public void logProjectAccessResult(JoinPoint joinPoint, Object result) {
        String username = getCurrentUsername();
        Long projectId = null;
         if (result != null) {
            try {
                java.lang.reflect.Method method = result.getClass().getMethod("getId");
                projectId = (Long) method.invoke(result);
            } catch (Exception e) {
                LOGGER.warn("Could not extract project id from project access response", e);
            }
        }
        LOGGER.info("Project accessed: Project ID {}, User: {}", projectId, username);
    }

    @AfterThrowing(pointcut = "projectAccess()", throwing = "error")
    public void logProjectAccessFailure(JoinPoint joinPoint, Throwable error) {
        String username = getCurrentUsername();
        Object[] args = joinPoint.getArgs();
        Long projectId = (Long) args[0]; // Assuming the first argument is the project ID
        LOGGER.error("Project access failed: Project ID: {}, User: {}, Error: {}", projectId, username, error.getMessage());
    }
    
    @Before("projectSharing()")
    public void logProjectSharing(JoinPoint joinPoint) {
        String username = getCurrentUsername();
        Object[] args = joinPoint.getArgs();
        Long projectId = null;
        String sharedWithUser = null;
        
        if (args.length > 1) {
            try {
                java.lang.reflect.Method projectIdMethod = args[0].getClass().getMethod("getProjectId");
                projectId = (Long) projectIdMethod.invoke(args[0]);

                java.lang.reflect.Method sharedWithUserMethod = args[1].getClass().getMethod("getUsername");
                sharedWithUser = (String) sharedWithUserMethod.invoke(args[1]);
            } catch (Exception e) {
                LOGGER.warn("Could not extract project ID or shared user from project sharing request", e);
            }
        }

        LOGGER.info("Project sharing attempt: Project ID: {}, Shared with User: {}, Initiator: {}", projectId, sharedWithUser, username);
    }

    @AfterReturning(pointcut = "projectSharing()", returning = "result")
    public void logProjectSharingResult(JoinPoint joinPoint, Object result) {
        String username = getCurrentUsername();
        // Extract relevant information from the result if needed
        LOGGER.info("Project shared successfully: User: {}", username);
    }

    @AfterThrowing(pointcut = "projectSharing()", throwing = "error")
    public void logProjectSharingFailure(JoinPoint joinPoint, Throwable error) {
        String username = getCurrentUsername();
        Object[] args = joinPoint.getArgs();
        Long projectId = null;
        String sharedWithUser = null;

        if (args.length > 1) {
            try {
                java.lang.reflect.Method projectIdMethod = args[0].getClass().getMethod("getProjectId");
                projectId = (Long) projectIdMethod.invoke(args[0]);

                java.lang.reflect.Method sharedWithUserMethod = args[1].getClass().getMethod("getUsername");
                sharedWithUser = (String) sharedWithUserMethod.invoke(args[1]);
            } catch (Exception e) {
                LOGGER.warn("Could not extract project ID or shared user from project sharing request", e);
            }
        }
        LOGGER.error("Project sharing failed: Project ID: {}, Shared with User: {}, Initiator: {}, Error: {}", projectId, sharedWithUser, username, error.getMessage());
    }

    @Before("userUpdate()")
    public void logUserUpdate(JoinPoint joinPoint) {
        String username = getCurrentUsername();
        Object[] args = joinPoint.getArgs();
        Long userId = null;

        if (args.length > 0 && args[0] != null) {
            try {
                java.lang.reflect.Method method = args[0].getClass().getMethod("getId");
                userId = (Long) method.invoke(args[0]);
            } catch (Exception e) {
                LOGGER.warn("Could not extract user ID from user update request", e);
            }
        }
        LOGGER.info("User update attempt: User ID: {}, Initiator: {}", userId, username);
    }

    @AfterReturning(pointcut = "userUpdate()", returning = "result")
    public void logUserUpdateResult(JoinPoint joinPoint, Object result) {
        String username = getCurrentUsername();
        // Extract relevant information from the result if needed, like updated user details
        LOGGER.info("User updated successfully: User: {}", username);
    }

    @AfterThrowing(pointcut = "userUpdate()", throwing = "error")
    public void logUserUpdateFailure(JoinPoint joinPoint, Throwable error) {
        String username = getCurrentUsername();
        Object[] args = joinPoint.getArgs();
        Long userId = null;
        if (args.length > 0 && args[0] != null) {
            try {
                java.lang.reflect.Method method = args[0].getClass().getMethod("getId");
                userId = (Long) method.invoke(args[0]);
            } catch (Exception e) {
                LOGGER.warn("Could not extract user ID from user update request", e);
            }
        }
        LOGGER.error("User update failed: User ID: {}, Initiator: {}, Error: {}", userId, username, error.getMessage());
    }

    private String getCurrentUsername() {
        // Retrieve the username from the security context
        try {
            org.springframework.security.core.context.SecurityContext context = org.springframework.security.core.context.SecurityContextHolder.getContext();
            org.springframework.security.core.Authentication authentication = context.getAuthentication();

            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }
        } catch (Exception e) {
            LOGGER.warn("Could not retrieve username from security context", e);
        }
        return "Unknown";
    }
}
