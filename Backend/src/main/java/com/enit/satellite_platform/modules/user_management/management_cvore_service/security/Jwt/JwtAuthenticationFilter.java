      package com.enit.satellite_platform.modules.user_management.management_cvore_service.security.Jwt;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.enit.satellite_platform.modules.user_management.normal_user_service.services.UserDetailsServiceImpl;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger; // Add Logger import
import org.slf4j.LoggerFactory; // Add LoggerFactory import

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class); // Declare logger

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

/**
 * Filters incoming HTTP requests to authenticate users based on JWT tokens.
 *
 * @param request      The HTTP request containing the JWT token in the Authorization header.
 * @param response     The HTTP response to be sent back to the client.
 * @param filterChain  The filter chain used to process the request.
 * @throws ServletException If an exception occurs that interferes with the filter's operations.
 * @throws IOException      If an I/O error occurs during the filtering process.
 *
 * This method extracts the JWT token from the Authorization header,
 * validates it, and if valid, retrieves the user details and sets the
 * authentication in the Spring SecurityContext. If the token is invalid
 * or any error occurs, it logs the error and continues with the filter chain.
 */

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = extractJwtToken(request);

            if (jwt != null && jwtUtil.validateToken(jwt)) {
                String username = jwtUtil.extractUsername(jwt); // Username is email in our case

                // Check if user is already authenticated for this request
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                    // If token is valid, configure Spring Security to manually set authentication
                    if (jwtUtil.validateToken(jwt)) { // Re-validate before setting context (optional but safer)
                        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authenticationToken
                                .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        // After setting the Authentication in the context, we specify
                        // that the current user is authenticated. So it passes the
                        // Spring Security Configurations successfully.
                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                        logger.debug("Set authentication for user: {}", username);
                    } else {
                         logger.warn("JWT Token validation failed for user: {}", username);
                    }
                } else if (username == null) {
                     logger.warn("Could not extract username from JWT token.");
                }
                // If authentication is already set, we don't need to do anything further here.
            } else if (jwt != null) {
                 logger.debug("JWT Token is invalid or expired.");
            }
            // If jwt is null, it means no token was provided, proceed without authentication.

        } catch (Exception e) {
            // Log specific exceptions if needed for better debugging
            logger.error("Could not set user authentication in security context", e);
        }

        // Continue the filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT token from the Authorization header of the given HTTP request.
     *
     * @param request The HTTP request containing the JWT token in the Authorization header.
     * @return The JWT token, or null if the header is missing or does not contain a valid token.
     */
    private String extractJwtToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

   
}
