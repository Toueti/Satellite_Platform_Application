package com.enit.satellite_platform.modules.user_management.management_cvore_service.services;

import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.RefreshToken;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.repositories.RefreshTokenRepository;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.security.device.DeviceInfo;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.security.device.DeviceManagementService;
import com.enit.satellite_platform.config.JwtConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RefreshTokenService {
    
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private JwtConfig jwtConfig;
    
    @Autowired
    private DeviceManagementService deviceManagementService;
    
    private static final String TOKEN_BLACKLIST_PREFIX = "blacklist:refresh:";
    private static final int MAX_ACTIVE_TOKENS_PER_USER = 5;
    
    @Transactional
    public RefreshToken createRefreshToken(String userId, HttpServletRequest request) {
        // First, register or update the device
        DeviceInfo deviceInfo = deviceManagementService.registerDevice(userId, request);
        
        // Check if user has reached max active tokens
        Date now = new Date();
        int activeTokens = refreshTokenRepository.countByUserIdAndIsRevokedFalseAndExpiryDateAfter(userId, now);
        if (activeTokens >= MAX_ACTIVE_TOKENS_PER_USER) {
            log.warn("User {} has reached maximum number of active refresh tokens", userId);
            revokeOldestTokenForUser(userId);
        }
        
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setToken(UUID.randomUUID().toString());
        
        // Set expiry based on device approval status
        if (deviceInfo.isApproved()) {
            refreshToken.setExpiryDate(new Date(System.currentTimeMillis() + jwtConfig.getExpirationTime() * 2));
        } else {
            // Shorter expiry for unapproved devices
            refreshToken.setExpiryDate(new Date(System.currentTimeMillis() + jwtConfig.getExpirationTime()));
        }
        
        refreshToken.setCreatedAt(now);
        refreshToken.setDevice(deviceInfo);
        refreshToken.setIpAddress(deviceInfo.getIpAddress());
        refreshToken.setUserAgent(deviceInfo.getUserAgent());
        
        return refreshTokenRepository.save(refreshToken);
    }
    
    public Optional<RefreshToken> findByToken(String token) {
        // Check blacklist first
        Boolean isBlacklisted = redisTemplate.hasKey(TOKEN_BLACKLIST_PREFIX + token);
        if (Boolean.TRUE.equals(isBlacklisted)) {
            log.warn("Attempt to use blacklisted refresh token");
            return Optional.empty();
        }
        
        return refreshTokenRepository.findByToken(token)
            .filter(RefreshToken::isValid)
            .filter(rt -> rt.getDevice() != null && rt.getDevice().isApproved());
    }
    
    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshToken -> {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            
            // Add to blacklist with TTL
            long ttl = refreshToken.getExpiryDate().getTime() - System.currentTimeMillis();
            if (ttl > 0) {
                redisTemplate.opsForValue().set(
                    TOKEN_BLACKLIST_PREFIX + token,
                    "revoked",
                    ttl,
                    java.util.concurrent.TimeUnit.MILLISECONDS
                );
            }
        });
    }
    
    @Transactional
    public void revokeAllUserTokens(String userId) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findValidTokensByUserId(userId, new Date());
        activeTokens.forEach(token -> {
            revokeToken(token.getToken());
        });
    }
    
    @Transactional
    public void revokeTokensForDevice(String deviceId) {
        List<RefreshToken> deviceTokens = refreshTokenRepository.findByDeviceId(deviceId);
        deviceTokens.forEach(token -> revokeToken(token.getToken()));
    }
    
    private void revokeOldestTokenForUser(String userId) {
        refreshTokenRepository.findValidTokensByUserId(userId, new Date()).stream()
            .min(java.util.Comparator.comparing(RefreshToken::getCreatedAt))
            .ifPresent(token -> revokeToken(token.getToken()));
    }
    
    @Transactional
    public void cleanupExpiredTokens() {
        Date now = new Date();
        List<RefreshToken> expiredTokens = refreshTokenRepository.findAllExpiredTokens(now);
        refreshTokenRepository.deleteAll(expiredTokens);
        log.info("Cleaned up {} expired refresh tokens", expiredTokens.size());
    }
}
