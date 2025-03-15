package com.enit.satellite_platform.user_management.config;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;

@Configuration
public class JwtConfig {

private static final Logger logger = LoggerFactory.getLogger(JwtConfig.class);

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expirationTime;

    @PostConstruct
    public void init() {
        logger.info("JWT Secret: {}", secretKey);
        logger.info("JWT Expiration: {}", expirationTime);
    }

    public String getSecretKey() {
        return secretKey;
    }

    public long getExpirationTime() {
        return expirationTime;
    }
}
