package com.enit.satellite_platform.modules.project_management.dto;

@lombok.Data
@lombok.EqualsAndHashCode(callSuper = true)
public class DeletedProjectDto extends ProjectDto {

    private String retentionDays;
    private String deletionDate;
    
}
