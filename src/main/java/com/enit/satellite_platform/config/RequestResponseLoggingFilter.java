package com.enit.satellite_platform.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@Order(1) // Ensure this filter runs early
public class RequestResponseLoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Skip logging for WebSocket upgrade requests or static resources if desired
        if (isWebSocketUpgradeRequest(httpRequest) || isStaticResourceRequest(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(httpRequest);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);

        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString().substring(0, 8); // Short unique ID for correlation

        // Log Request
        logRequest(requestWrapper, requestId);

        try {
            chain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            // Log Response
            logResponse(responseWrapper, requestId, duration);
            // IMPORTANT: Copy the response body back to the original response
            responseWrapper.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request, String requestId) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullUri = uri + (queryString == null ? "" : "?" + queryString);
        String remoteAddr = request.getRemoteAddr();
        String requestBody = getRequestBody(request);

        logger.info(">>> Request [{}]: {} {} from {} - Body: {}",
                requestId, method, fullUri, remoteAddr, requestBody.isEmpty() ? "[empty]" : requestBody);
        // Add header logging if needed: Collections.list(request.getHeaderNames()).forEach(h -> logger.debug("Header {}: {}", h, request.getHeader(h)));
    }

    private void logResponse(ContentCachingResponseWrapper response, String requestId, long duration) {
        int status = response.getStatus();
        String responseBody = getResponseBody(response);

        logger.info("<<< Response [{}]: Status {} in {}ms - Body: {}",
                requestId, status, duration, responseBody.isEmpty() ? "[empty]" : responseBody);
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] buf = request.getContentAsByteArray();
        if (buf.length > 0) {
            try {
                return new String(buf, 0, buf.length, request.getCharacterEncoding() != null ? request.getCharacterEncoding() : StandardCharsets.UTF_8.name());
            } catch (Exception e) {
                logger.warn("Could not read request body: {}", e.getMessage());
                return "[Could not read body]";
            }
        }
        return "";
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] buf = response.getContentAsByteArray();
        if (buf.length > 0) {
            try {
                return new String(buf, 0, buf.length, response.getCharacterEncoding() != null ? response.getCharacterEncoding() : StandardCharsets.UTF_8.name());
            } catch (Exception e) {
                logger.warn("Could not read response body: {}", e.getMessage());
                return "[Could not read body]";
            }
        }
        return "";
    }

     private boolean isWebSocketUpgradeRequest(HttpServletRequest request) {
        String upgradeHeader = request.getHeader("Upgrade");
        String connectionHeader = request.getHeader("Connection");
        return "websocket".equalsIgnoreCase(upgradeHeader) &&
               (connectionHeader != null && connectionHeader.toLowerCase().contains("upgrade"));
    }

     private boolean isStaticResourceRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // Add patterns for your static resources (css, js, images, etc.)
        return uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/images/") || uri.endsWith(".ico");
     }

    // init and destroy methods are optional
}
