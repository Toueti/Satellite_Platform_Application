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
        private String defaultValue;
        private String description;
        private String lastUpdated;
    }
