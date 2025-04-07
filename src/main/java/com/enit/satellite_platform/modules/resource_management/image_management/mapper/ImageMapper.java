package com.enit.satellite_platform.modules.resource_management.image_management.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.enit.satellite_platform.modules.resource_management.image_management.dto.ImageDTO;
import com.enit.satellite_platform.modules.resource_management.image_management.models.Image;
import com.enit.satellite_platform.modules.resource_management.image_management.repositories.ImageRepository.ImageMetadataProjection; // Import the projection

import org.bson.types.ObjectId;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ImageMapper {

    @Mapping(source = "project.projectId", target = "projectId", qualifiedByName = "objectIdToString")
    @Mapping(source = "imageData", target = "file", ignore = true)
    ImageDTO toDTO(Image image);

    // New mapping for the projection
    @Mapping(source = "project.projectId", target = "projectId", qualifiedByName = "objectIdToString")
    @Mapping(target = "file", ignore = true) // Ensure file is ignored here too
    ImageDTO toDTO(ImageMetadataProjection projection);

    @Mapping(target = "project", ignore = true)
    @Mapping(target = "results", ignore = true)
    @Mapping(target = "requestTime", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(source = "file", target = "imageData", qualifiedByName = "multipartFileToByteArray")
    Image toEntity(ImageDTO imageDTO);

    List<ImageDTO> toDTOList(List<Image> images);

    // New mapping for list of projections
    List<ImageDTO> projectionToDTOList(List<ImageMetadataProjection> projections);

    @Named("objectIdToString")
    default String objectIdToString(ObjectId id) {
        return id.toHexString();
    }

    @Named("multipartFileToByteArray")
    default byte[] multipartFileToByteArray(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        return file.getBytes();
    }
}
