package com.enit.satellite_platform.config.rateLimiter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collection;

@Component
@Slf4j
public class RateLimitingFilter implements Filter {

    private final RateLimiter rateLimiter;
    private final RateLimitProperties properties;

    public RateLimitingFilter(RateLimiter rateLimiter, RateLimitProperties properties) {
        this.rateLimiter = rateLimiter;
        this.properties = properties;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
            HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

            // Check if the user is an admin
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && isAdmin(authentication.getAuthorities())) {
                // Admin users bypass rate limiting
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }

            // Apply rate limiting for non-admin users
            String key = extractKey(httpRequest);
            RateLimitResult result = rateLimiter.checkLimit(key);

            // Add rate limit headers
            httpResponse.setHeader("X-RateLimit-Remaining", String.valueOf(result.getRemainingRequests()));
            httpResponse.setHeader("X-RateLimit-Reset", String.valueOf(result.getResetTimeMs()));

            if (!result.isAllowed()) {
                log.warn("Rate limit exceeded for client: {}", key);
                sendRateLimitExceededResponse(httpResponse);
                return;
            }

            filterChain.doFilter(servletRequest, servletResponse);
        } catch (Exception e) {
            log.error("Error in rate limiting filter", e);
            throw e;
        }
    }

    private String extractKey(HttpServletRequest request) {
        String key = request.getHeader("X-Client-ID");
        return (key != null && !key.isEmpty()) ? key : request.getRemoteAddr();
    }

    private void sendRateLimitExceededResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"Rate limit exceeded\", \"retry_after\": " + 
            properties.getTimeWindowMillis() / 1000 + "}");
    }

    private boolean isAdmin(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
    }
}
