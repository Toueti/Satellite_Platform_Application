package com.enit.satellite_platform.config.dto;

    import lombok.AllArgsConstructor;
    import lombok.Data;
    import lombok.NoArgsConstructor;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class ManageablePropertyDto {
        private String key;
        private String currentValue;
        private String defaultValue; // Value from application.properties or system env
        private String description;
        private String lastUpdated; // ISO String format (e.g., "2023-10-27T10:15:30") or null
    }
