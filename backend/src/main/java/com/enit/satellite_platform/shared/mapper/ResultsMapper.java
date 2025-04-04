package com.enit.satellite_platform.shared.mapper;

import java.util.Map;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.enit.satellite_platform.modules.resource_management.dto.ProcessingResponse;
import com.enit.satellite_platform.modules.resource_management.image_management.dto.resultsSaveRequest;
import com.enit.satellite_platform.modules.resource_management.image_management.models.ProcessingResults;


@Mapper(componentModel = "spring")
public interface ResultsMapper {
    @SuppressWarnings("unchecked")
    default Map<String, Object> map(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }
	@Mapping(target = "image", ignore = true)
    @Mapping(target = "resultsId", ignore = true)
    @Mapping(target = "cacheKey", ignore = true)
    ProcessingResults toEntity(resultsSaveRequest geeSaveRequest);

    @Mapping(source = "image.imageId", target = "imageId")
    resultsSaveRequest toDTO(ProcessingResults geeResults);

    //mape ProcessingResponse to ProcessingResults
    @Mapping(target = "image", ignore = true)
    @Mapping(target = "resultsId", ignore = true)
    @Mapping(target = "cacheKey", ignore = true)
    ProcessingResults toEntity(ProcessingResponse processingResponse);

    // Map ProcessingResults to ProcessingResponse
    @Mapping(source = "image.imageId", target = "imageId") // Map nested imageId
    @Mapping(target = "status", ignore = true) // Status is likely contextual, not stored
    @Mapping(target = "message", ignore = true) // Message is likely contextual, not stored
    ProcessingResponse toProcessingResponse(ProcessingResults processingResults);
}
