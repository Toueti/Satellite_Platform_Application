package com.enit.satellite_platform.config;

/**
 * Configuration class for JWT (JSON Web Token) settings.
 * This class manages the configuration for JWT generation and validation,
 * including the secret key and token expiration time.
 *
 * The class is annotated with @RefreshScope to allow dynamic updates of
 * JWT-related properties without requiring application restart.
 * It logs the JWT configuration details during initialization for debugging purposes.
 */
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;

@Configuration
@RefreshScope
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
