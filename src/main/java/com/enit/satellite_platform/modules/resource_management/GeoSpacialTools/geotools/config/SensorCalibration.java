package com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.geotools.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class to store calibration parameters for different sensors
 */
public class SensorCalibration {
    private Map<String, Double> ndviParameters; // Parameters for NDVI calculation adjustments
    private Map<String, Double> eviParameters; // Parameters for EVI calculation adjustments
    private Map<String, DetectionThresholds> detectionThresholds; // Thresholds for object detection
    private Map<String, double[]> spectralSignatures; // Spectral signatures for classification
    private AtmosphericCorrection redBandCorrection; // Atmospheric correction for red band
    private AtmosphericCorrection nirBandCorrection; // Atmospheric correction for NIR band

    // Constructor initializes all maps and potentially default corrections
    public SensorCalibration() {
        ndviParameters = new HashMap<>();
        eviParameters = new HashMap<>();
        detectionThresholds = new HashMap<>();
        spectralSignatures = new HashMap<>(); // Initialize spectral signatures map
        // Initialize with default null corrections, can be set later
        redBandCorrection = null; 
        nirBandCorrection = null;
    }

    /**
     * Set custom NDVI calculation parameters
     * 
     * @param sensorName       Name of the sensor
     * @param lowerBoundOffset Adjustment for lower bound of vegetation
     * @param upperBoundOffset Adjustment for upper bound of vegetation
     */
    public void setNDVIParameters(String sensorName, double lowerBoundOffset, double upperBoundOffset) {
        ndviParameters.put(sensorName + "_lower", lowerBoundOffset);
        ndviParameters.put(sensorName + "_upper", upperBoundOffset);
    }

    /**
     * Set custom EVI calculation parameters
     * 
     * @param sensorName       Name of the sensor
     * @param gainFactor       Gain factor for EVI calculation
     * @param canopyAdjustment Canopy background adjustment
     */
    public void setEVIParameters(String sensorName, double gainFactor, double canopyAdjustment) {
        eviParameters.put(sensorName + "_gain", gainFactor);
        eviParameters.put(sensorName + "_canopy", canopyAdjustment);
    }

    /**
     * Nested class to store detection thresholds
     */
    public static class DetectionThresholds {
        public double waterThreshold;
        public double roadThreshold;
        public double buildingThreshold;

        public DetectionThresholds(double waterThreshold, double roadThreshold, double buildingThreshold) {
            this.waterThreshold = waterThreshold;
            this.roadThreshold = roadThreshold;
            this.buildingThreshold = buildingThreshold;
        }
    }

    /**
     * Nested class to store atmospheric correction parameters.
     */
    public static class AtmosphericCorrection {
        private double gain;
        private double offset;
        private double darkObjectValue;

        public AtmosphericCorrection(double gain, double offset, double darkObjectValue) {
            this.gain = gain;
            this.offset = offset;
            this.darkObjectValue = darkObjectValue;
        }

        // Getters for atmospheric correction parameters
        public double getGain() { return gain; }
        public double getOffset() { return offset; }
        public double getDarkObjectValue() { return darkObjectValue; }
    }

     /**
      * Set atmospheric correction parameters for the red band.
      * @param correction AtmosphericCorrection object for the red band.
      */
     public void setRedBandCorrection(AtmosphericCorrection correction) {
         this.redBandCorrection = correction;
     }

     /**
      * Set atmospheric correction parameters for the NIR band.
      * @param correction AtmosphericCorrection object for the NIR band.
      */
     public void setNirBandCorrection(AtmosphericCorrection correction) {
         this.nirBandCorrection = correction;
     }

     /**
      * Get atmospheric correction parameters for the red band.
      * @return AtmosphericCorrection object or null if not set.
      */
     public AtmosphericCorrection getRedBandCorrection() {
         return redBandCorrection;
     }

     /**
      * Get atmospheric correction parameters for the NIR band.
      * @return AtmosphericCorrection object or null if not set.
      */
     public AtmosphericCorrection getNirBandCorrection() {
         return nirBandCorrection;
     }

     /**
      * Set spectral signatures for a sensor.
      * @param signatures Map where keys are class names (e.g., "water") and values are spectral signature arrays.
      */
     public void setSpectralSignatures(Map<String, double[]> signatures) {
         this.spectralSignatures = signatures;
     }

     /**
      * Get spectral signatures associated with this calibration.
      * @return Map of spectral signatures or null/empty if not set.
      */
     public Map<String, double[]> getSpectralSignatures() {
         return spectralSignatures;
     }

    /**
     * Set detection thresholds for a specific sensor
     * 
     * @param sensorName Name of the sensor
     * @param thresholds Detection thresholds for water, road, and building
     *                   detection
     */
    public void setDetectionThresholds(String sensorName, DetectionThresholds thresholds) {
        detectionThresholds.put(sensorName, thresholds);
    }

    /**
     * Get NDVI parameters for a specific sensor
     * 
     * @param sensorName Name of the sensor
     * @return Map of NDVI parameters
     */
    public Map<String, Double> getNDVIParameters(String sensorName) {
        return Map.of(
                "lower", ndviParameters.getOrDefault(sensorName + "_lower", -0.2),
                "upper", ndviParameters.getOrDefault(sensorName + "_upper", 0.5));
    }

    /**
     * Get EVI parameters for a specific sensor
     * 
     * @param sensorName Name of the sensor
     * @return Map of EVI parameters
     */
    public Map<String, Double> getEVIParameters(String sensorName) {
        return Map.of(
                "gain", eviParameters.getOrDefault(sensorName + "_gain", 2.5),
                "canopy", eviParameters.getOrDefault(sensorName + "_canopy", 1.0));
    }

    /**
     * Get detection thresholds for a specific sensor
     * 
     * @param sensorName Name of the sensor
     * @return Detection thresholds
     */
    public DetectionThresholds getDetectionThresholds(String sensorName) {
        return detectionThresholds.getOrDefault(
                sensorName,
                new DetectionThresholds(-0.05, 0.3, 0.4)); // Default thresholds if none specific are set
    }
}
