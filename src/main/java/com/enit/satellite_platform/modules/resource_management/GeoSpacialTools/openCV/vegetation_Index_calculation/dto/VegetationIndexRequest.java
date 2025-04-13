package com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.openCV.vegetation_Index_calculation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for vegetation index calculation request parameters
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VegetationIndexRequest {
    @JsonProperty("index_type")
    private String indexType;
    
    @JsonProperty("red_band")
    private int redBand = 1;
    
    @JsonProperty("nir_band")
    private int nirBand = 2;
    
    @JsonProperty("blue_band")
    private Integer blueBand;
    
    @JsonProperty("g")
    private Float G;
    
    @JsonProperty("c1")
    private Float C1;
    
    @JsonProperty("c2")
    private Float C2;
    
    @JsonProperty("l")
    private Float L;

    // Default constructor
    public VegetationIndexRequest() {}

    // Constructor with required fields
    public VegetationIndexRequest(String indexType, int redBand, int nirBand) {
        this.indexType = indexType;
        this.redBand = redBand;
        this.nirBand = nirBand;
    }

    // Getters and Setters
    public String getIndexType() {
        return indexType;
    }

    public void setIndexType(String indexType) {
        this.indexType = indexType;
    }

    public int getRedBand() {
        return redBand;
    }

    public void setRedBand(int redBand) {
        this.redBand = redBand;
    }

    public int getNirBand() {
        return nirBand;
    }

    public void setNirBand(int nirBand) {
        this.nirBand = nirBand;
    }

    public Integer getBlueBand() {
        return blueBand;
    }

    public void setBlueBand(Integer blueBand) {
        this.blueBand = blueBand;
    }

    public Float getG() {
        return G;
    }

    public void setG(Float g) {
        this.G = g;
    }

    public Float getC1() {
        return C1;
    }

    public void setC1(Float c1) {
        this.C1 = c1;
    }

    public Float getC2() {
        return C2;
    }

    public void setC2(Float c2) {
        this.C2 = c2;
    }

    public Float getL() {
        return L;
    }

    public void setL(Float l) {
        this.L = l;
    }
}
