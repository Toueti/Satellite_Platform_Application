package com.enit.satellite_platform.config.model;

    import lombok.Data;
    import lombok.NoArgsConstructor;
    import org.springframework.data.annotation.Id;
    import org.springframework.data.mongodb.core.mapping.Document;

    import java.time.LocalDateTime;

    @Document(collection = "config_properties")
    @Data
    @NoArgsConstructor
    public class ConfigProperty {

        @Id
        private String id; // Use the configuration key as the ID for simplicity and uniqueness

        private String value;

        private String description; // Optional description for the admin UI

        private String defaultValue; // Stores the original default value

        private LocalDateTime lastUpdated;

        // Constructor updated to include defaultValue
        public ConfigProperty(String id, String value, String description, String defaultValue) {
            this.id = id;
            this.value = value;
            this.description = description;
            this.defaultValue = defaultValue; // Set the default value
            this.lastUpdated = LocalDateTime.now();
        }

        // Keep NoArgsConstructor for frameworks/libraries
    }
