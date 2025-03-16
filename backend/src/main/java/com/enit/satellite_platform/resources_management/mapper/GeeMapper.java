package com.enit.satellite_platform.resources_management.mapper;

import com.enit.satellite_platform.resources_management.dto.GeeSaveRequest;
import com.enit.satellite_platform.resources_management.models.GeeResults;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface GeeMapper {
	@Mapping(target = "image", ignore = true)
    @Mapping(target = "resultsId", ignore = true)
    GeeResults toEntity(GeeSaveRequest geeSaveRequest);

    @Mapping(source = "image.imageId", target = "imageId")
    GeeSaveRequest toDTO(GeeResults geeResults);
}
