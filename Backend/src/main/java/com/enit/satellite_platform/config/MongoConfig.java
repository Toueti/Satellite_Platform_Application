package com.enit.satellite_platform.config;

// Removed PermissionLevelConverter and PermissionLevelWritingConverter imports as they are no longer needed
/**
 * MongoDB configuration class that sets up MongoDB client, database factory, and template.
 * This class provides the core configuration for MongoDB connectivity in the application.
 *
 * Features:
 * - Creates and configures MongoDB client with connection settings
 * - Sets up MongoDB database factory for database operations
 * - Configures MongoDB template for high-level MongoDB operations
 * - Supports runtime configuration refresh with @RefreshScope
 * - Provides detailed logging for debugging connection issues
 * - Implements connection string validation and error handling
 *
 * Note: GridFS support is commented out but can be enabled if needed for file storage.
 */
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@RefreshScope // ! Make all beans defined in this class refreshable
// Enable MongoDB repositories, scanning all sub-packages under modules
@EnableMongoRepositories
public class MongoConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoConfig.class);

    // Inject the MongoDB URI property. Spring will automatically use the value
    // from the Environment, including overrides from DatabasePropertySource.
    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Bean
    public MongoClient mongoClient() {
        log.info("Creating MongoClient bean with URI from environment...");
        ConnectionString connectionString = new ConnectionString(mongoUri);
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                // Add any other custom settings here if needed
                .build();
        log.debug("MongoClient created for URI: {}", connectionString.getConnectionString()); // Log the actual URI used
        return MongoClients.create(mongoClientSettings);
    }

    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient) {
        log.info("Creating MongoDatabaseFactory bean...");
        // Extract database name from the URI or use a default/fallback if necessary
        String databaseName = new ConnectionString(mongoUri).getDatabase();
        if (databaseName == null || databaseName.isEmpty()) {
            log.error("Database name could not be determined from MongoDB URI: {}", mongoUri);
            // Handle this error appropriately - throw exception or use a default
            throw new IllegalStateException("MongoDB database name is missing in the connection URI.");
        }
        log.debug("Using database name: {}", databaseName);
        return new SimpleMongoClientDatabaseFactory(mongoClient, databaseName);
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory) {
        log.info("Creating MongoTemplate bean...");
        return new MongoTemplate(mongoDatabaseFactory);
    }

    @Bean
    @Primary
    public MongoCustomConversions mongoCustomConversions() { // Removed converter parameters
        // Return an empty list or potentially add other converters if needed in the future
        return new MongoCustomConversions(Arrays.asList( 
                // Removed permissionLevelConverter,
                // Removed permissionLevelWritingConverter
        ));
    }
}
