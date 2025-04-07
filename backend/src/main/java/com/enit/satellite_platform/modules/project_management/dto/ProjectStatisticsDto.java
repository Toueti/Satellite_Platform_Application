package com.enit.satellite_platform.modules.project_management.dto;

import org.bson.types.ObjectId;

import java.util.Date;
import java.util.Map;

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
    private Map<ObjectId, Long> imagesPerProject;
    /**
     * A map containing the last access time for each project, keyed by project ID.
     */
    private Map<ObjectId, Date> lastAcccessTime;

    /**
     * Constructs a ProjectStatisticsDto with the given statistics.
     *
     * @param totalProjects    The total number of projects.
     * @param imagesPerProject A map of project IDs to the number of images in each project.
     * @param lastAcccessTime  A map of project IDs to their last access time.
     */
    public ProjectStatisticsDto(long totalProjects, Map<ObjectId, Long> imagesPerProject, Map<ObjectId, Date> lastAcccessTime) {
        this.totalProjects = totalProjects;
        this.imagesPerProject = imagesPerProject;
        this.lastAcccessTime = lastAcccessTime;
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
    public Map<ObjectId, Long> getImagesPerProject() {
        return imagesPerProject;
    }

    /**
     * Sets the map of images per project.
     *
     * @param imagesPerProject The map of images per project to set.
     */
    public void setImagesPerProject(Map<ObjectId, Long> imagesPerProject) {
        this.imagesPerProject = imagesPerProject;
    }

    /**
     * Gets the map of last access times for projects.
     *
     * @return The map of last access times.
     */
    public Map<ObjectId, Date> getlastAcccessTime() {
        return lastAcccessTime;
    }

    /**
     * Sets the map of last access times for projects.
     *
     * @param lastAcccessTime The map of last access times to set.
     */
    public void setlastAcccessTime(Map<ObjectId, Date> lastAcccessTime) {
        this.lastAcccessTime = lastAcccessTime;
    }
}
