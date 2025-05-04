package com.enit.satellite_platform.modules.resource_management.image_management.entities;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.enit.satellite_platform.shared.model.SoftDeletable;
import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.PrePersist;
import lombok.Data;

@Data
@Document(collection = "processing_results")
@CompoundIndex(name = "type_status_date", def = "{'processing_type': 1, 'status': 1, 'date': -1}")
public class ProcessingResults implements SoftDeletable {
                                                                                                  

    @Id
    private ObjectId resultsId;

    @Field("data")
    private Map<String, Object> data;

    @Field("storage_identifier")
    private String storageIdentifier;

    @Field("storage_type")
    private String storageType;

    @Field("file_size")
    private long fileSize;

    @Field("date")
    @Indexed
    private LocalDateTime date;

    @Field("processing_type")
    @Indexed
    private ProcessingType type;

    @Field("status")
    @Indexed
    private ProcessingStatus status;

    @Field("error_message")
    private String errorMessage;

    @DBRef
    @Field("image")
    @JsonBackReference // Add annotation to manage the 'child' side of the relationship
    private Image image;

    @Field("deleted")
    private boolean deleted = false; // Soft delete flag

    @Field("deleted_at")
    private Date deletedAt; // Timestamp for soft delete

    /**
     * Constants for commonly used keys in the data Map.
     * These constants help prevent typos and provide better IDE support.
     */
    public static class DataKeys {
        // Vegetation Indices
        public static final String INDEX_TYPE = "indexType";
        public static final String RED_BAND = "redBand";
        public static final String NIR_BAND = "nirBand";
        public static final String BLUE_BAND = "blueBand";
        public static final String MEAN_VALUE = "meanValue";
        public static final String MIN_VALUE = "minValue";
        public static final String MAX_VALUE = "maxValue";

        // GEE
        public static final String GEE_SCRIPT_ID = "geeScriptId";
        public static final String START_DATE = "startDate";
        public static final String END_DATE = "endDate";
        public static final String CLOUD_COVER = "cloudCover";
        public static final String REGION = "region";
        public static final String SCALE = "scale";

        // Common
        public static final String PROCESSING_TIME_MS = "processingTimeMs";
        public static final String PARAMETERS = "parameters";
        public static final String WARNINGS = "warnings";
    }

    @PrePersist
    protected void onCreate() {
        date = LocalDateTime.now();
    }

    public String getResultsId() {
        return this.resultsId.toHexString();
    }

    @Override
    public ObjectId getId() {
        return this.resultsId;
    }

}
