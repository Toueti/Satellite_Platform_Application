package com.enit.satellite_platform.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
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
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories; // Added import

@Configuration
@RefreshScope //! Make all beans defined in this class refreshable
// Enable MongoDB repositories, scanning all sub-packages under modules
@EnableMongoRepositories
public class MongoConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoConfig.class);

    // Inject the MongoDB URI property. Spring will automatically use the value
    // from the Environment, including overrides from DatabasePropertySource.
    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Bean
    @Primary //! Mark this as the primary MongoClient bean if others exist
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
    @Primary //! Mark this as the primary MongoDatabaseFactory bean
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
    @Primary // Mark this as the primary MongoTemplate bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory, MappingMongoConverter converter) {
        log.info("Creating MongoTemplate bean...");
        // The MappingMongoConverter is typically auto-configured by Spring Boot Data MongoDB
        return new MongoTemplate(mongoDatabaseFactory, converter);
    }

    // ? Optional: If you need custom type mapping or converters, define the MappingMongoConverter bean here as well.
    //! Spring Boot Data MongoDB usually provides a default one.
    // @Bean
    // public MappingMongoConverter mappingMongoConverter(MongoDatabaseFactory factory, MongoMappingContext context) {
    //     DbRefResolver dbRefResolver = new DefaultDbRefResolver(factory);
    //     MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, context);
    //     // Customize converter if needed (e.g., custom conversions)
    //     return converter;
    // }
}
