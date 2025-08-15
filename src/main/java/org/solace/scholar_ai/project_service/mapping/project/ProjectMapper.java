package org.solace.scholar_ai.project_service.mapping.project;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.solace.scholar_ai.project_service.dto.project.CreateProjectDto;
import org.solace.scholar_ai.project_service.dto.project.ProjectDto;
import org.solace.scholar_ai.project_service.dto.project.UpdateProjectDto;
import org.solace.scholar_ai.project_service.model.project.Project;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    ProjectMapper INSTANCE = Mappers.getMapper(ProjectMapper.class);

    @Mapping(target = "status", source = "status", qualifiedByName = "statusEnumToString")
    @Mapping(target = "topics", source = "topics", qualifiedByName = "stringToList")
    @Mapping(target = "tags", source = "tags", qualifiedByName = "stringToList")
    ProjectDto toDto(Project entity);

    @Mapping(target = "id", ignore = true) // ID will be generated
    @Mapping(target = "createdAt", ignore = true) // Auto-generated
    @Mapping(target = "updatedAt", ignore = true) // Auto-generated
    @Mapping(target = "status", source = "status", qualifiedByName = "stringToStatusEnum")
    @Mapping(target = "topics", source = "topics", qualifiedByName = "listToString")
    @Mapping(target = "tags", source = "tags", qualifiedByName = "listToString")
    Project toEntity(ProjectDto dto);

    @Mapping(target = "id", source = "dto.id")
    @Mapping(target = "name", source = "dto.name")
    @Mapping(target = "description", source = "dto.description")
    @Mapping(target = "domain", source = "dto.domain")
    @Mapping(target = "userId", source = "dto.userId")
    @Mapping(target = "progress", source = "dto.progress")
    @Mapping(target = "totalPapers", source = "dto.totalPapers")
    @Mapping(target = "activeTasks", source = "dto.activeTasks")
    @Mapping(target = "lastActivity", source = "dto.lastActivity")
    @Mapping(target = "isStarred", source = "dto.isStarred")
    @Mapping(target = "createdAt", ignore = true) // Don't update creation time
    @Mapping(target = "updatedAt", ignore = true) // Auto-generated
    @Mapping(target = "status", source = "dto.status", qualifiedByName = "stringToStatusEnum")
    @Mapping(target = "topics", source = "dto.topics", qualifiedByName = "listToString")
    @Mapping(target = "tags", source = "dto.tags", qualifiedByName = "listToString")
    Project toEntityForUpdate(ProjectDto dto, Project existingEntity);

    @Named("statusEnumToString")
    default String statusEnumToString(Project.Status status) {
        return status != null ? status.name() : null;
    }

    @Named("stringToStatusEnum")
    default Project.Status stringToStatusEnum(String status) {
        if (status == null || status.trim().isEmpty()) {
            return Project.Status.ACTIVE; // Default status
        }
        try {
            return Project.Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Project.Status.ACTIVE; // Fallback to default
        }
    }

    @Named("stringToList")
    default List<String> stringToList(String str) {
        if (str == null || str.trim().isEmpty()) return null;
        return Arrays.asList(str.split(","));
    }

    @Named("listToString")
    default String listToString(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        return String.join(",", list);
    }

    // Additional mapping methods for CreateProjectDto and UpdateProjectDto
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "status", expression = "java(Project.Status.ACTIVE)")
    @Mapping(target = "progress", constant = "0")
    @Mapping(target = "totalPapers", constant = "0")
    @Mapping(target = "activeTasks", constant = "0")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastActivity", ignore = true)
    @Mapping(target = "isStarred", constant = "false")
    @Mapping(target = "topics", source = "dto.topics", qualifiedByName = "listToString")
    @Mapping(target = "tags", source = "dto.tags", qualifiedByName = "listToString")
    @Mapping(target = "name", source = "dto.name")
    @Mapping(target = "description", source = "dto.description")
    @Mapping(target = "domain", source = "dto.domain")
    Project fromCreateDto(CreateProjectDto dto, UUID userId);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "totalPapers", ignore = true)
    @Mapping(target = "activeTasks", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", source = "status", qualifiedByName = "stringToStatusEnum")
    @Mapping(target = "topics", source = "topics", qualifiedByName = "listToString")
    @Mapping(target = "tags", source = "tags", qualifiedByName = "listToString")
    Project fromUpdateDto(UpdateProjectDto dto);
}
