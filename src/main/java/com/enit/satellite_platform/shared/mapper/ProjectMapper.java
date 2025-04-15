package com.enit.satellite_platform.shared.mapper;

import java.util.List;

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


    @Mapping(target = "ownerEmail", expression = "java(project.getOwner() != null ? project.getOwner().getEmail() : null)") // Handle potential null owner
    @Mapping(source = "id", target = "id", qualifiedByName = "objectIdToString")
    ProjectDto toDTO(Project project);

    @Mapping(target = "ownerEmail", expression = "java(project.getOwner() != null ? project.getOwner().getEmail() : null)") // Handle potential null owner
    @Mapping(source = "id", target = "id", qualifiedByName = "objectIdToString")
    @Mapping(target = "deletionDate", source = "deletedAt")
    DeletedProjectDto toDeletedProjectDto(Project project);

    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "archived", ignore = true)
    @Mapping(target = "archivedDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastAccessedTime", ignore = true)
    @Mapping(target = "metadata", ignore = true)
    @Mapping(target = "projectDirectory", ignore = true)
    @Mapping(target = "sharedUsers", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "retentionDays", ignore = true)
    Project toEntity(ProjectDto projectDTO);

/**
 * Converts a Page of Project entities to a Page of ProjectDto objects.
 * Ignores the "dummy" field during mapping.
 *
 * @param projects the Page of Project entities to be converted
 * @return a Page of ProjectDto objects
 */

    @Mapping(target = "dummy", ignore = true)
    @Named("toDTOList")
    default Page<ProjectDto> toDTOPage(Page<Project> projects) {
        return projects.map(this::toDTO);
    }

    @Named("toDeletedProjectDtoPage")
    default Page<DeletedProjectDto> toDeletedProjectDtoPage(Page<Project> project) {
        return project.map(this::toDeletedProjectDto);
    }

    default ProjectDto mapToDTO(Project project) {
        return toDTO(project);
    }

    List<Project> toEntityList(List<ProjectDto> projectDTOs);
}
