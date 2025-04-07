package com.enit.satellite_platform.modules.resource_management.image_management.models;

import lombok.Data;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.enit.satellite_platform.config.cache_handler.general_cache_handler.CacheableEntity;

import jakarta.persistence.PrePersist;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Document(collection = "processing_results")
public class ProcessingResults implements CacheableEntity{

    @Id
    private ObjectId resultsId;
    
    @Field("cache_key")
    private String cacheKey;

    @Field("data")
    private Map<String,Object> data;

    @Field("date")
    private LocalDateTime date;

    @Field("type")
    private String type;

    @Field("status")
    private ProcessingStatus status; // Added status field

    @DBRef
    @Field("image")
    private Image image;

    @PrePersist
    protected void onCreate(){
        date = LocalDateTime.now();
    }
}
