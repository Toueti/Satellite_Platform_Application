package com.enit.satellite_platform.config;

/**
 * Configuration class for loading environment variables from a .env file.
 * This class is responsible for loading environment variables during application startup
 * and setting them as system properties to be accessible throughout the application.
 *
 * Uses the dotenv library to read environment variables from a .env file and
 * make them available to the application through System.getProperty().
 */
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
@Configuration
public class EnvConfig {

    @PostConstruct
    public void loadEnv() {
        Dotenv dotenv = Dotenv.configure().load();
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });
    }
}
