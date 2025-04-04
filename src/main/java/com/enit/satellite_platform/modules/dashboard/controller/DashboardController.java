package com.enit.satellite_platform.modules.dashboard.controller;

import com.enit.satellite_platform.modules.dashboard.dto.DashboardStatsDto;
import com.enit.satellite_platform.modules.dashboard.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private DashboardService dashboardService;

    /**
     * Retrieves dashboard statistics for the currently authenticated user.
     *
     * @return ResponseEntity containing the DashboardStatsDto.
     */
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()") // Ensure the user is logged in
    public ResponseEntity<DashboardStatsDto> getUserDashboardStats() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName(); // Assumes email is used as username in security context

        logger.info("Received request for dashboard stats for user: {}", userEmail);

        try {
            DashboardStatsDto stats = dashboardService.getDashboardStats(userEmail);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            // Catch potential exceptions like UsernameNotFoundException or others from the service
            logger.error("Error generating dashboard stats for user {}: {}", userEmail, e.getMessage(), e);
            // Consider returning a more specific error response based on the exception type
            return ResponseEntity.internalServerError().build();
        }
    }
}
