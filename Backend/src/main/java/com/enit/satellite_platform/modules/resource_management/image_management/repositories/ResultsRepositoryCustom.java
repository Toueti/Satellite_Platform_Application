package com.enit.satellite_platform.modules.resource_management.image_management.repositories;

import com.enit.satellite_platform.modules.resource_management.image_management.entities.ProcessingResults;
import java.util.List;

/**
 * Custom repository interface for ProcessingResults to define complex queries.
 */
public interface ResultsRepositoryCustom {

    /**
     * Finds ProcessingResults associated with a specific project ID, excluding deleted ones,
     * using a custom aggregation query to handle nested DBRefs.
     *
     * @param projectId The ID of the project.
     * @return A list of non-deleted ProcessingResults for the given project ID.
     */
    List<ProcessingResults> findByProjectIdAndDeletedFalseCustom(String projectId);
}
