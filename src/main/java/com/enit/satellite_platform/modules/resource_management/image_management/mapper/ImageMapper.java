package com.enit.satellite_platform.modules.resource_management.image_management.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import com.enit.satellite_platform.modules.resource_management.image_management.dto.ImageDTO;
import com.enit.satellite_platform.modules.resource_management.image_management.entities.Image;
import com.enit.satellite_platform.modules.resource_management.image_management.repositories.ImageRepository.ImageMetadataProjection;
import com.enit.satellite_platform.shared.mapper.ObjectIdMapper;

import org.bson.types.ObjectId;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Mapper(componentModel = "spring", uses = ObjectIdMapper.class)
public interface ImageMapper {

     ImageMapper INSTANCE = Mappers.getMapper(ImageMapper.class);

    // Utility methods

    @Named("objectIdToString")
    default String objectIdToString(ObjectId objectId) {
        return objectId != null ? objectId.toString() : null;
    }

    @Named("multipartFileToGridFsFileId")
    default String multipartFileToGridFsFileId(MultipartFile file) throws IOException {
        return null;
    }

    // Mapping for full Image entity (no file content in entity, fetched separately)
    @Mapping(source = "project.id", target = "projectId")
    @Mapping(target = "file", ignore = true)
    ImageDTO toDTO(Image image);

    // Mapping for projection
    @Mapping(source = "project.id", target = "projectId")
    @Mapping(target = "file", ignore = true) // File not included in projection
    ImageDTO toDTO(ImageMetadataProjection projection);

    // Mapping DTO to entity (for saving new images)
    @Mapping(source = "projectId", target = "project.id", qualifiedByName = "stringToObjectId")
    @Mapping(target = "results", ignore = true)
    @Mapping(target = "requestTime", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "metadata", ignore = true)
    Image toEntity(ImageDTO imageDTO);

    // List mappings
    List<ImageDTO> toDTOList(List<Image> images);

    List<ImageDTO> projectionToDTOList(List<ImageMetadataProjection> projections);

}