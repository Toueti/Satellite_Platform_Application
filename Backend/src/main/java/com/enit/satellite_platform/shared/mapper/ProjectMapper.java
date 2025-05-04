package com.enit.satellite_platform.shared.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.data.domain.Page;

import com.enit.satellite_platform.modules.project_management.dto.DeletedProjectDto;
import com.enit.satellite_platform.modules.project_management.dto.ProjectDto;
import com.enit.satellite_platform.modules.project_management.entities.Project;
import com.enit.satellite_platform.modules.resource_management.image_management.mapper.ImageMapper;

@Mapper(componentModel = "spring", uses = ImageMapper.class)
public interface ProjectMapper {

    // --- Custom ID Conversion Methods ---


    default ObjectId stringToObjectId(String id) {
        return id != null ? new ObjectId(id) : null;
    }

    // --- Standard Mapping Methods ---


    @Mapping(target = "ownerEmail", expression = "java(project.getOwner() != null ? project.getOwner().getEmail() : null)")
    @Mapping(source = "id", target = "id", qualifiedByName = "objectIdToString")
    ProjectDto toDTO(Project project);

    @Mapping(target = "ownerEmail", expression = "java(project.getOwner() != null ? project.getOwner().getEmail() : null)")
    @Mapping(source = "id", target = "id", qualifiedByName = "objectIdToString")
    @Mapping(target = "deletionDate", source = "deletedAt")
    DeletedProjectDto toDeletedProjectDto(Project project);

    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "archived", ignore = true)
    @Mapping(target = "archivedDate", ignore = true)
    @Mapping(target = "metadata", ignore = true)
    @Mapping(target = "projectDirectory", ignore = true)
    @Mapping(target = "sharedUsers", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "retentionDays", ignore = true)
    @Mapping(target = "images", ignore = true)
    Project toEntity(ProjectDto projectDTO);

/**
 * Converts a Page of Project entities to a Page of ProjectDto objects.
 * Ignores the "dummy" field during mapping.
 *
 * @param projects the Page of Project entities to be converted
 * @return a Page of ProjectDto objects
 */

    @Mapping(target = "dummy", ignore = true)
    @Named("toDTOPage")
    default Page<ProjectDto> toDTOPage(Page<Project> projects) {
        return projects.map(this::toDTO);
    }

    @Named("toDeletedProjectDtoPage")
    default Page<DeletedProjectDto> toDeletedProjectDtoPage(Page<Project> project) {
        return project.map(this::toDeletedProjectDto);
    }

    @Named("toDTOList")
    default List<ProjectDto> toDTOList(List<Project> projects) {
        return projects.stream().map(this::toDTO).collect(Collectors.toList());
    }

    List<Project> toEntityList(List<ProjectDto> projectDTOs);
}
