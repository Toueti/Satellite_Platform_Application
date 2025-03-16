package com.enit.satellite_platform.user_management.security;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.enit.satellite_platform.user_management.service.UserDetailsServiceImpl;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

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

    @SuppressWarnings("null")
    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Extract JWT token from the Authorization header
            String token = extractJwtToken(request);

            if (token != null && jwtUtil.validateToken(token)) {
                // Extract username from token
                String username = jwtUtil.extractUsername(token);

                // Load user details
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Create Authentication object
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set authentication in SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e);
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