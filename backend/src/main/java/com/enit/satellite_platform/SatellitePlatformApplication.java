package com.enit.satellite_platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * The main entry point for the Satellite Platform application. This class configures and runs the Spring Boot
 * application. It uses Spring Boot's auto-configuration feature but excludes the DataSourceAutoConfiguration
 * since the application uses MongoDB. Auditing and asynchronous method execution are also enabled.
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableMongoAuditing
@EnableAsync
public class SatellitePlatformApplication {
    /**
     * The main method to run the Satellite Platform application.
     *
     * @param args Command line arguments passed to the application.
     */
    public static void main(String[] args) {
        SpringApplication.run(SatellitePlatformApplication.class, args);
    }
}
