package com.enit.satellite_platform.modules.user_management.management_cvore_service.security.device;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Basic implementation of DeviceIdentityProvider that uses User-Agent and IP address
 * for device identification.
 */
@Service
public class BasicDeviceIdentityProvider implements DeviceIdentityProvider {
    
    @Override
    public String getDeviceIdentifier(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String ipAddress = getClientIpAddress(request);
        
        // Create a simple unique identifier by combining IP and User-Agent hash
        return String.format("%s_%d", ipAddress, userAgent.hashCode());
    }
    
    @Override
    public DeviceMetadata getDeviceMetadata(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String ipAddress = getClientIpAddress(request);
        
        return new DeviceMetadata(userAgent, ipAddress, LocalDateTime.now());
    }
    
    /**
     * Extracts the client IP address from the request, taking into account proxy headers.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] PROXY_HEADERS = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };
        
        for (String header : PROXY_HEADERS) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // If the IP contains multiple addresses, take the first one
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        // If no proxy headers are found, use the remote address
        return request.getRemoteAddr();
    }
}
