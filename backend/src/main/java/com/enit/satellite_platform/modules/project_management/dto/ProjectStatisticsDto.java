package com.enit.satellite_platform.modules.project_management.dto;

import org.bson.types.ObjectId;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;

/**
 * DTO for representing project statistics.
 */
public class ProjectStatisticsDto {
    /**
     * The total number of projects.
     */
    private long totalProjects;
    /**
     * A map containing the number of images per project, keyed by project ID.
     */
    private Map<String, Long> imagesPerProject;
    /**
     * A map containing the last access time for each project, keyed by project ID.
     */
    private Map<String, Date> lastAcccessTime;

    /**
     * Constructs a ProjectStatisticsDto with the given statistics.
     *
     * @param totalProjects    The total number of projects.
     * @param imagesPerProject A map of project IDs to the number of images in each project.
     * @param lastAcccessTime  A map of project IDs to their last access time.
     */
    public ProjectStatisticsDto(long totalProjects, Map<ObjectId, Long> imagesPerProject, Map<ObjectId, Date> lastAcccessTime) {
        this.totalProjects = totalProjects;
        this.imagesPerProject = convertObjectIdMapToStringMap(imagesPerProject);
        this.lastAcccessTime = convertObjectIdMapToStringMap(lastAcccessTime);
    }

    /**
     * Converts a map with ObjectId keys to a map with String keys.
     *
     * @param map The map to convert.
     * @return A new map with String keys.
     */
    private <T> Map<String, T> convertObjectIdMapToStringMap(Map<ObjectId, T> map) {
        Map<String, T> result = new HashMap<>();
        if (map != null) {
            for (Map.Entry<ObjectId, T> entry : map.entrySet()) {
                result.put(entry.getKey().toString(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * Gets the total number of projects.
     *
     * @return The total number of projects.
     */
    public long getTotalProjects() {
        return totalProjects;
    }

    /**
     * Sets the total number of projects.
     *
     * @param totalProjects The total number of projects to set.
     */
    public void setTotalProjects(long totalProjects) {
        this.totalProjects = totalProjects;
    }

    /**
     * Gets the map of images per project.
     *
     * @return The map of images per project.
     */
    public Map<String, Long> getImagesPerProject() {
        return imagesPerProject;
    }

    /**
     * Sets the map of images per project.
     *
     * @param imagesPerProject The map of images per project to set.
     */
    public void setImagesPerProject(Map<String, Long> imagesPerProject) {
        this.imagesPerProject = imagesPerProject;
    }

    /**
     * Gets the map of last access times for projects.
     *
     * @return The map of last access times.
     */
    public Map<String, Date> getlastAcccessTime() {
        return lastAcccessTime;
    }

    /**
     * Sets the map of last access times for projects.
     *
     * @param lastAcccessTime The map of last access times to set.
     */
    public void setlastAcccessTime(Map<String, Date> lastAcccessTime) {
        this.lastAcccessTime = lastAcccessTime;
    }
}
