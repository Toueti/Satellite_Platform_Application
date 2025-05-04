package com.enit.satellite_platform.modules.resource_management.image_management.repositories;

import com.enit.satellite_platform.modules.resource_management.image_management.entities.ProcessingResults;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Implementation of the custom repository interface for ProcessingResults.
 */
@Repository
public class ResultsRepositoryImpl implements ResultsRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public ResultsRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<ProcessingResults> findByProjectIdAndDeletedFalseCustom(String projectId) {
        // Ensure projectId is a valid ObjectId string before conversion
        if (!ObjectId.isValid(projectId)) {
            // Handle invalid ID format appropriately, e.g., throw exception or return empty list
            // For now, returning empty list to avoid errors if an invalid ID is passed.
            return List.of();
        }
        ObjectId projectObjectId = new ObjectId(projectId);

        // $lookup: Join processing_results with images collection
        // The 'image' field in ProcessingResults stores the DBRef (containing $id) to the Image document.
        // MongoDB stores DBRefs like: "image" : { "$ref" : "images", "$id" : ObjectId("...") }
        // We need to lookup based on the localField 'image.$id' matching the foreignField '_id' in 'images'.
        // Note: Spring Data MongoDB handles DBRef fields slightly differently in aggregations.
        // We specify the local field simply as 'image'. Spring Data understands this refers to the DBRef.
        // The foreign field in the 'images' collection is '_id'.
        LookupOperation lookupOperation = Aggregation.lookup("images", "image.$id", "_id", "imageData");

        // $unwind: Deconstruct the imageData array created by $lookup
        // Since each ProcessingResult links to one Image, this should result in one document per match.
        UnwindOperation unwindOperation = Aggregation.unwind("imageData");

        // $match: Filter the results
        // 1. Match where the project ID within the joined image data equals the provided projectId.
        //    The project field within Image is also a DBRef: "project" : { "$ref" : "projects", "$id" : ObjectId("...") }
        //    So we match on 'imageData.project.$id'.
        // 2. Match where the 'deleted' flag in ProcessingResults is false.
        MatchOperation matchOperation = Aggregation.match(
            Criteria.where("imageData.project.$id").is(projectObjectId)
                    .and("deleted").is(false)
        );

        // Build the aggregation pipeline
        Aggregation aggregation = Aggregation.newAggregation(
            lookupOperation,
            unwindOperation,
            matchOperation
        );

        // Execute the aggregation
        return mongoTemplate.aggregate(aggregation, "processing_results", ProcessingResults.class).getMappedResults();
    }
}
