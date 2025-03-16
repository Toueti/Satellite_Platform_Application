package com.enit.satellite_platform.resources_management.mapper;

import com.enit.satellite_platform.resources_management.dto.ImageDTO;
import com.enit.satellite_platform.resources_management.models.Image;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.bson.types.ObjectId;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ImageMapper {

    @Mapping(source = "project.projectID", target = "projectId", qualifiedByName = "objectIdToString")
    ImageDTO toDTO(Image image);

    @Mapping(target = "project", ignore = true)
    @Mapping(target = "geeResults", ignore = true)
    @Mapping(target = "requestTime", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Image toEntity(ImageDTO imageDTO);

    List<ImageDTO> toDTOList(List<Image> images);

    @Named("objectIdToString")
    default String objectIdToString(ObjectId id) {
        return id.toHexString();
    }
}
