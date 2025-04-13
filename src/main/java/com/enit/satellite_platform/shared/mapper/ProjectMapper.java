package com.enit.satellite_platform.shared.mapper;

import java.util.List;

import org.bson.types.ObjectId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import com.enit.satellite_platform.modules.project_management.dto.ProjectDTO;
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
    ProjectDTO toDTO(Project project);

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
    Project toEntity(ProjectDTO projectDTO);

    List<ProjectDTO> toDTOList(List<Project> projects);

    default Page<ProjectDTO> toDTOPage(Page<Project> projects) {
        return projects.map(this::toDTO);
    }

    List<Project> toEntityList(List<ProjectDTO> projectDTOs);
}
