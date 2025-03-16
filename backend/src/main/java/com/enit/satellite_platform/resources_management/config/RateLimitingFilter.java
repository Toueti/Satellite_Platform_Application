package com.enit.satellite_platform.resources_management.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1) // Apply this filter before others
public class RateLimitingFilter implements Filter {

    private final RateLimiter rateLimiter = new RateLimiter(10, 60000); // 10 requests per minute

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        String key = httpRequest.getRemoteAddr(); // Use IP address as the key

        if (!rateLimiter.isAllowed(key)) {
            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.getWriter().write("Rate limit exceeded");
            return;
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }
}
/*package com.enit.satellite_platform.resources_management.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1)
public class RateLimitingFilter implements Filter {

    @Autowired
    private RedisRateLimiter redisRateLimiter;

    @Value("${rateLimit.maxRequests}")
    private int maxRequests;

    @Value("${rateLimit.timeWindowMillis}")
    private long timeWindowMillis;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        String key = httpRequest.getHeader("X-Client-ID"); // Custom header
        if (key == null || key.isEmpty()) {
            key = httpRequest.getRemoteAddr(); // Fallback to IP if header is missing
        }

        if (!redisRateLimiter.isAllowed(key, maxRequests, timeWindowMillis)) {
            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.getWriter().write("Rate limit exceeded");
            return;
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }
}
 */