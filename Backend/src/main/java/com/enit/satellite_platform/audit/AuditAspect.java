package com.enit.satellite_platform.audit;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.enit.satellite_platform.modules.project_management.entities.Project;

@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger("com.enit.satellite_platform.audit"); // Logger for audit events

    @Autowired
    private AuditService auditService;

    // --- Action Type Constants ---
    private static final String LOGIN_ATTEMPT = "LOGIN_ATTEMPT";
    private static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    private static final String LOGIN_FAILURE = "LOGIN_FAILURE";
    private static final String PROJECT_CREATE_ATTEMPT = "PROJECT_CREATE_ATTEMPT";
    private static final String PROJECT_CREATE_SUCCESS = "PROJECT_CREATE_SUCCESS";
    private static final String PROJECT_CREATE_FAILURE = "PROJECT_CREATE_FAILURE";
    private static final String PROJECT_ACCESS_ATTEMPT = "PROJECT_ACCESS_ATTEMPT";
    private static final String PROJECT_ACCESS_SUCCESS = "PROJECT_ACCESS_SUCCESS";
    private static final String PROJECT_ACCESS_FAILURE = "PROJECT_ACCESS_FAILURE";
    private static final String PROJECT_SHARE_ATTEMPT = "PROJECT_SHARE_ATTEMPT";
    private static final String PROJECT_SHARE_SUCCESS = "PROJECT_SHARE_SUCCESS";
    private static final String PROJECT_SHARE_FAILURE = "PROJECT_SHARE_FAILURE";
    private static final String USER_UPDATE_ATTEMPT = "USER_UPDATE_ATTEMPT";
    private static final String USER_UPDATE_SUCCESS = "USER_UPDATE_SUCCESS";
    private static final String USER_UPDATE_FAILURE = "USER_UPDATE_FAILURE";
    // Add more as needed: IMAGE_UPLOAD_ATTEMPT, PROCESSING_REQUEST_ATTEMPT, etc.


    // Pointcut for login attempts (adjust controller/method name if changed)
    @Pointcut("execution(* com.enit.satellite_platform.modules.user_management.controllers.AuthController.login(..))")
    public void loginAttempt() {}

    // Pointcut for project creation
    @Pointcut("execution(* com.enit.satellite_platform.modules.project_management.services.ProjectService.createProject(..))")
    public void projectCreation() {}

    // Pointcut for project access (getProjectById seems deprecated, using getProject)
    @Pointcut("execution(* com.enit.satellite_platform.modules.project_management.services.ProjectService.getProject(..))")
    public void projectAccess() {}

     // Pointcut for project sharing
    @Pointcut("execution(* com.enit.satellite_platform.modules.project_management.services.ProjectService.shareProject(..))")
    public void projectSharing() {}

    // Pointcut for user updates (adjust service/method name if changed)
    @Pointcut("execution(* com.enit.satellite_platform.modules.user_management.user_service.services.UserService.updateUser(..))")
    public void userUpdate() {}

    // --- Advice Methods ---

    @Before("loginAttempt()")
    public void auditLoginAttempt(JoinPoint joinPoint) {
        String username = extractUsernameFromLoginArgs(joinPoint.getArgs());
        // userId is not known before login attempt
        auditService.recordEvent(null, username, LOGIN_ATTEMPT, null);
    }

    @AfterReturning(pointcut = "loginAttempt()", returning = "result")
    public void auditLoginSuccess(JoinPoint joinPoint, Object result) {
        // Assuming successful login result contains user details
        String username = extractUsernameFromResult(result);
        String userId = extractUserIdFromResult(result); // Implement this helper
        auditService.recordEvent(userId, username, LOGIN_SUCCESS, null);
    }

    @AfterThrowing(pointcut = "loginAttempt()", throwing = "error")
    public void auditLoginFailure(JoinPoint joinPoint, Throwable error) {
        String username = extractUsernameFromLoginArgs(joinPoint.getArgs());
        // userId is not known on failure
        auditService.recordEvent(null, username, LOGIN_FAILURE, null);
        log.info("Login failure for {}: {}", username, error.getMessage());
    }

    @Before("projectCreation()")
    public void auditProjectCreationAttempt(JoinPoint joinPoint) {
        AuditPrincipal principal = getCurrentPrincipal();
        String projectName = extractProjectNameFromArgs(joinPoint.getArgs());
        auditService.recordEvent(principal.userId(), principal.username(), PROJECT_CREATE_ATTEMPT, projectName); // Use name as target temporarily
    }

    @AfterReturning(pointcut = "projectCreation()", returning = "result")
    public void auditProjectCreationSuccess(JoinPoint joinPoint, Object result) {
        AuditPrincipal principal = getCurrentPrincipal();
        String projectId = extractProjectIdFromResult(result);
        auditService.recordEvent(principal.userId(), principal.username(), PROJECT_CREATE_SUCCESS, projectId);
    }

    @AfterThrowing(pointcut = "projectCreation()", throwing = "error")
    public void auditProjectCreationFailure(JoinPoint joinPoint, Throwable error) {
        AuditPrincipal principal = getCurrentPrincipal();
        String projectName = extractProjectNameFromArgs(joinPoint.getArgs());
        auditService.recordEvent(principal.userId(), principal.username(), PROJECT_CREATE_FAILURE, projectName);
        log.info("Project creation failure for {} by {}: {}", projectName, principal.username(), error.getMessage());
    }

    @Before("projectAccess()")
    public void auditProjectAccessAttempt(JoinPoint joinPoint) {
        AuditPrincipal principal = getCurrentPrincipal();
        String projectId = extractProjectIdFromArgs(joinPoint.getArgs(), 0); // Assuming ID is first arg
        auditService.recordEvent(principal.userId(), principal.username(), PROJECT_ACCESS_ATTEMPT, projectId);
    }

    @AfterReturning(pointcut = "projectAccess()", returning = "result")
    public void auditProjectAccessSuccess(JoinPoint joinPoint, Object result) {
        AuditPrincipal principal = getCurrentPrincipal();
        String projectId = extractProjectIdFromResult(result);
        auditService.recordEvent(principal.userId(), principal.username(), PROJECT_ACCESS_SUCCESS, projectId);
    }

    @AfterThrowing(pointcut = "projectAccess()", throwing = "error")
    public void auditProjectAccessFailure(JoinPoint joinPoint, Throwable error) {
        AuditPrincipal principal = getCurrentPrincipal();
        String projectId = extractProjectIdFromArgs(joinPoint.getArgs(), 0);
        auditService.recordEvent(principal.userId(), principal.username(), PROJECT_ACCESS_FAILURE, projectId);
        log.info("Project access failure for {} by {}: {}", projectId, principal.username(), error.getMessage());
    }

    @Before("projectSharing()")
    public void auditProjectSharingAttempt(JoinPoint joinPoint) {
        AuditPrincipal principal = getCurrentPrincipal();
        Object[] args = joinPoint.getArgs();
        String projectId = (args.length > 0 && args[0] instanceof String) ? (String) args[0] : null; // Assuming projectId is String now
        String sharedWithEmail = (args.length > 1 && args[1] instanceof String) ? (String) args[1] : null;
        auditService.recordEvent(principal.userId(), principal.username(), PROJECT_SHARE_ATTEMPT, projectId + " -> " + sharedWithEmail);
    }

    @AfterReturning(pointcut = "projectSharing()", returning = "result")
    public void auditProjectSharingSuccess(JoinPoint joinPoint) {
        AuditPrincipal principal = getCurrentPrincipal();
        Object[] args = joinPoint.getArgs();
        String projectId = (args.length > 0 && args[0] instanceof String) ? (String) args[0] : null;
        String sharedWithEmail = (args.length > 1 && args[1] instanceof String) ? (String) args[1] : null;
        auditService.recordEvent(principal.userId(), principal.username(), PROJECT_SHARE_SUCCESS, projectId + " -> " + sharedWithEmail);
    }

    @AfterThrowing(pointcut = "projectSharing()", throwing = "error")
    public void auditProjectSharingFailure(JoinPoint joinPoint, Throwable error) {
        AuditPrincipal principal = getCurrentPrincipal();
        Object[] args = joinPoint.getArgs();
        String projectId = (args.length > 0 && args[0] instanceof String) ? (String) args[0] : null;
        String sharedWithEmail = (args.length > 1 && args[1] instanceof String) ? (String) args[1] : null;
        auditService.recordEvent(principal.userId(), principal.username(), PROJECT_SHARE_FAILURE, projectId + " -> " + sharedWithEmail);
        log.info("Project sharing failure for {} by {}: {}", projectId, principal.username(), error.getMessage());
    }

    @Before("userUpdate()")
    public void auditUserUpdateAttempt(JoinPoint joinPoint) {
        AuditPrincipal principal = getCurrentPrincipal();
        String targetUserId = extractUserIdFromUserUpdateArgs(joinPoint.getArgs()); // Implement this
        auditService.recordEvent(principal.userId(), principal.username(), USER_UPDATE_ATTEMPT, targetUserId);
    }

    @AfterReturning(pointcut = "userUpdate()", returning = "result")
    public void auditUserUpdateSuccess(JoinPoint joinPoint, Object result) {
        AuditPrincipal principal = getCurrentPrincipal();
        String targetUserId = extractUserIdFromResult(result); // Assuming result is the updated User
        auditService.recordEvent(principal.userId(), principal.username(), USER_UPDATE_SUCCESS, targetUserId);
    }

    @AfterThrowing(pointcut = "userUpdate()", throwing = "error")
    public void auditUserUpdateFailure(JoinPoint joinPoint, Throwable error) {
        AuditPrincipal principal = getCurrentPrincipal();
        String targetUserId = extractUserIdFromUserUpdateArgs(joinPoint.getArgs());
        auditService.recordEvent(principal.userId(), principal.username(), USER_UPDATE_FAILURE, targetUserId);
        log.info("User update failure for {} by {}: {}", targetUserId, principal.username(), error.getMessage());
    }


    // --- Helper Methods ---

    private record AuditPrincipal(String userId, String username) {}

    private AuditPrincipal getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication.getPrincipal() instanceof String)) {
            Object principal = authentication.getPrincipal();
            String username = authentication.getName(); 
            String userId = null;

            if (principal instanceof UserDetails userDetails) {
                 username = userDetails.getUsername();
                 // Attempt to get userId if UserDetails is our custom User class
                 if (principal instanceof com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.User customUser && customUser.getId() != null) {
                     userId = customUser.getId();
                 }
            }
            // Add other principal types if necessary

            return new AuditPrincipal(userId, username);
        }
        return new AuditPrincipal(null, "Unknown/Anonymous");
    }

    private String extractUsernameFromLoginArgs(Object[] args) {
        if (args.length > 0 && args[0] != null) {
            // Assuming first arg is LoginRequest DTO with getUsername()
            try {
                return (String) args[0].getClass().getMethod("getUsername").invoke(args[0]);
            } catch (Exception e) {
                log.warn("Could not extract username from login request args", e);
            }
        }
        return "Unknown";
    }

     private String extractUsernameFromResult(Object result) {
        if (result != null) {
            // Try common methods like getUsername() or getName()
            try { return (String) result.getClass().getMethod("getUsername").invoke(result); } catch (Exception e) {}
            try { return (String) result.getClass().getMethod("getName").invoke(result); } catch (Exception e) {}
             // If result is UserDetails
            if (result instanceof UserDetails userDetails) { return userDetails.getUsername();}
        }
        return "Unknown";
    }

    private String extractUserIdFromResult(Object result) {
         if (result != null) {
            // Try common method getId()
            try {
                Object id = result.getClass().getMethod("getId").invoke(result);
                return (id != null) ? id.toString() : null;
            } catch (Exception e) {}
             // If result is our custom User class
            if (result instanceof com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.User customUser && customUser.getId() != null) {
                 return customUser.getId().toString(); // Convert ObjectId to String
            }
             // If result is directly an ObjectId (e.g., from some other method)
             if (result instanceof ObjectId objectId) {
                 return objectId.toString();
             }
        }
        return null;
    }

     private String extractProjectNameFromArgs(Object[] args) {
        if (args.length > 0 && args[0] instanceof Project project) {
            return project.getProjectName();
        }
        // Add reflection fallback if needed, but prefer direct type checking
        return "Unknown";
    }

    private String extractProjectIdFromResult(Object result) {
        if (result instanceof Project project && project.getId() != null) {
            return project.getId().toString();
        }
        // Add reflection fallback if needed, e.g., for methods returning just the ID
        if (result instanceof ObjectId objectId) {
             return objectId.toString();
        }
         if (result instanceof String stringId) {
             try {
                 return new ObjectId(stringId).toString(); // Validate if it's an ObjectId string
             } catch (IllegalArgumentException e) { /* ignore */ }
         }
        return null;
    }

     private String extractProjectIdFromArgs(Object[] args, int index) {
        if (args.length > index && args[index] != null) {
            if (args[index] instanceof ObjectId objectId) {
                return objectId.toString();
            } else if (args[index] instanceof String stringId) {
                 try {
                     // Attempt to parse as ObjectId string, return if valid
                     return new ObjectId(stringId).toString();
                 } catch (IllegalArgumentException e) {
                     log.warn("Argument at index {} is a String but not a valid ObjectId: {}", index, stringId);
                     // Optionally return the string itself if non-ObjectId strings are possible targets
                     // return stringId;
                 }
            } else {
                 log.warn("Argument at index {} is not an ObjectId or String: type={}", index, args[index].getClass().getName());
            }
        }
        return null;
    }

     private String extractUserIdFromUserUpdateArgs(Object[] args) {
         // This depends heavily on the actual signature of updateUser method
         if (args.length > 0 && args[0] != null) {
             // Example: if first arg is User object
             if (args[0] instanceof com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.User user && user.getId() != null) {
                 return user.getId().toString(); // Convert ObjectId to String
             }
             // Example: if first arg is userId as ObjectId
             else if (args[0] instanceof ObjectId objectId) {
                 return objectId.toString();
             }
             // Example: if first arg is userId as String (potentially ObjectId string)
             else if (args[0] instanceof String stringId) {
                  try {
                      // Validate if it's an ObjectId string and return
                      return new ObjectId(stringId).toString();
                  } catch (IllegalArgumentException e) {
                      log.warn("User update arg is String but not a valid ObjectId: {}", stringId);
                      // Decide if non-ObjectId strings are valid user identifiers here
                      // return stringId;
                  }
             }
             // Example: if first arg is userId as Long (less likely with MongoDB)
             else if (args[0] instanceof Long longId) {
                 return longId.toString();
             }
             else {
                 log.warn("Could not determine user ID type from user update args: type={}", args[0].getClass().getName());
             }
         }
         return null;
     }
}
