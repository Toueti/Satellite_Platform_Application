package com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.openCV.vegetation_Index_calculation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * DTO that matches the Python script's ProcessingResult class
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VegetationIndexResult {
    @JsonProperty("index_type")
    private String indexType;
    
    @JsonProperty("start_time")
    private String startTime;
    
    @JsonProperty("end_time")
    private String endTime;
    
    @JsonProperty("processing_duration")
    private double processingDuration;
    
    @JsonProperty("statistics")
    private Map<String, Double> statistics;

    // Default constructor
    public VegetationIndexResult() {}

    // Getters and Setters
    public String getIndexType() {
        return indexType;
    }

    public void setIndexType(String indexType) {
        this.indexType = indexType;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public double getProcessingDuration() {
        return processingDuration;
    }

    public void setProcessingDuration(double processingDuration) {
        this.processingDuration = processingDuration;
    }

    public Map<String, Double> getStatistics() {
        return statistics;
    }

    public void setStatistics(Map<String, Double> statistics) {
        this.statistics = statistics;
    }

    // outputFilePath field and its getter/setter were removed.

    @Override
    public String toString() {
        return String.format(
            "VegetationIndexResult{indexType='%s', duration=%.2fs, stats=%s}",
            indexType,
            processingDuration,
            statistics != null ? statistics.toString() : "null"
        );
    }
}
