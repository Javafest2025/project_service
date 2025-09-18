package org.solace.scholar_ai.project_service.mapping.readinglist;

import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.solace.scholar_ai.project_service.dto.readinglist.AddReadingListItemDto;
import org.solace.scholar_ai.project_service.dto.readinglist.ReadingListItemDto;
import org.solace.scholar_ai.project_service.dto.readinglist.UpdateReadingListItemDto;
import org.solace.scholar_ai.project_service.model.readinglist.ReadingListItem;

@Mapper(componentModel = "spring")
public interface ReadingListItemMapper {

    ReadingListItemMapper INSTANCE = Mappers.getMapper(ReadingListItemMapper.class);

    ReadingListItemDto toDto(ReadingListItem entity);

    @Mapping(target = "id", ignore = true) // ID will be generated
    @Mapping(target = "projectId", source = "projectId")
    @Mapping(target = "paperId", source = "dto.paperId")
    @Mapping(target = "status", constant = "PENDING") // Default to pending
    @Mapping(
            target = "priority",
            expression =
                    "java(dto.priority() != null ? ReadingListItem.Priority.valueOf(dto.priority().toUpperCase()) : ReadingListItem.Priority.MEDIUM)")
    @Mapping(
            target = "difficulty",
            expression =
                    "java(dto.difficulty() != null ? ReadingListItem.Difficulty.valueOf(dto.difficulty().toUpperCase()) : ReadingListItem.Difficulty.MEDIUM)")
    @Mapping(
            target = "relevance",
            expression =
                    "java(dto.relevance() != null ? ReadingListItem.Relevance.valueOf(dto.relevance().toUpperCase()) : ReadingListItem.Relevance.MEDIUM)")
    @Mapping(target = "estimatedTime", source = "dto.estimatedTime")
    @Mapping(target = "actualTime", ignore = true) // Not set on creation
    @Mapping(target = "notes", source = "dto.notes")
    @Mapping(target = "tags", source = "dto.tags")
    @Mapping(target = "rating", ignore = true) // Not set on creation
    @Mapping(target = "readingProgress", constant = "0") // Default to 0
    @Mapping(target = "readCount", constant = "0") // Default to 0
    @Mapping(target = "isBookmarked", constant = "false") // Default to false
    @Mapping(target = "isRecommended", constant = "false") // Default to false
    @Mapping(target = "recommendedBy", ignore = true) // Not set on creation
    @Mapping(target = "recommendedReason", ignore = true) // Not set on creation
    @Mapping(target = "addedAt", ignore = true) // Auto-generated
    @Mapping(target = "startedAt", ignore = true) // Not set on creation
    @Mapping(target = "completedAt", ignore = true) // Not set on creation
    @Mapping(target = "lastReadAt", ignore = true) // Not set on creation
    @Mapping(target = "updatedAt", ignore = true) // Auto-generated
    @Mapping(target = "paper", ignore = true) // Not mapped
    ReadingListItem fromAddDto(AddReadingListItemDto dto, UUID projectId);

    @Mapping(target = "id", source = "entity.id") // Preserve existing ID
    @Mapping(target = "projectId", source = "entity.projectId") // Preserve existing project ID
    @Mapping(target = "paperId", source = "entity.paperId") // Preserve existing paper ID
    @Mapping(
            target = "status",
            expression =
                    "java(dto.status() != null ? ReadingListItem.Status.valueOf(dto.status().toUpperCase()) : entity.getStatus())")
    @Mapping(
            target = "priority",
            expression =
                    "java(dto.priority() != null ? ReadingListItem.Priority.valueOf(dto.priority().toUpperCase()) : entity.getPriority())")
    @Mapping(
            target = "difficulty",
            expression =
                    "java(dto.difficulty() != null ? ReadingListItem.Difficulty.valueOf(dto.difficulty().toUpperCase()) : entity.getDifficulty())")
    @Mapping(
            target = "relevance",
            expression =
                    "java(dto.relevance() != null ? ReadingListItem.Relevance.valueOf(dto.relevance().toUpperCase()) : entity.getRelevance())")
    @Mapping(target = "estimatedTime", source = "dto.estimatedTime")
    @Mapping(target = "actualTime", source = "dto.actualTime")
    @Mapping(target = "notes", source = "dto.notes")
    @Mapping(target = "tags", source = "dto.tags")
    @Mapping(target = "rating", source = "dto.rating")
    @Mapping(
            target = "readingProgress",
            expression = "java(dto.readingProgress() != null ? dto.readingProgress() : entity.getReadingProgress())")
    @Mapping(target = "readCount", ignore = true) // Don't update read count
    @Mapping(
            target = "isBookmarked",
            expression = "java(dto.isBookmarked() != null ? dto.isBookmarked() : entity.getIsBookmarked())")
    @Mapping(target = "isRecommended", ignore = true) // Don't update recommendation status
    @Mapping(target = "recommendedBy", ignore = true) // Don't update recommendation source
    @Mapping(target = "recommendedReason", ignore = true) // Don't update recommendation reason
    @Mapping(target = "addedAt", ignore = true) // Don't update creation time
    @Mapping(target = "startedAt", ignore = true) // Handle separately in service
    @Mapping(target = "completedAt", ignore = true) // Handle separately in service
    @Mapping(target = "lastReadAt", ignore = true) // Handle separately in service
    @Mapping(target = "updatedAt", ignore = true) // Auto-generated
    @Mapping(target = "paper", ignore = true) // Not mapped
    ReadingListItem fromUpdateDto(UpdateReadingListItemDto dto, ReadingListItem entity);
}
