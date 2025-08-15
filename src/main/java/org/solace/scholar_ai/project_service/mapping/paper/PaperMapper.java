package org.solace.scholar_ai.project_service.mapping.paper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.solace.scholar_ai.project_service.dto.paper.CreatePaperDto;
import org.solace.scholar_ai.project_service.dto.paper.PaperDto;
import org.solace.scholar_ai.project_service.dto.paper.PaperMetadataDto;
import org.solace.scholar_ai.project_service.dto.paper.UpdatePaperDto;
import org.solace.scholar_ai.project_service.mapping.author.AuthorMapper;
import org.solace.scholar_ai.project_service.model.paper.Paper;

@Mapper(
        componentModel = "spring",
        uses = {AuthorMapper.class})
public interface PaperMapper {

    PaperMapper INSTANCE = Mappers.getMapper(PaperMapper.class);

    // Map from PaperMetadataDto to Paper entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "correlationId", ignore = true)
    @Mapping(target = "paperAuthors", source = "authors", qualifiedByName = "authorsToPaperAuthors")
    @Mapping(target = "externalIds", source = "externalIds", qualifiedByName = "externalIdsMapToEntities")
    @Mapping(target = "venue", source = ".", qualifiedByName = "mapVenue")
    @Mapping(target = "metrics", source = ".", qualifiedByName = "mapMetrics")
    @Mapping(target = "publicationTypes", source = "publicationTypes", qualifiedByName = "listToString")
    @Mapping(target = "fieldsOfStudy", source = "fieldsOfStudy", qualifiedByName = "listToString")
    Paper fromMetadataDto(PaperMetadataDto dto);

    // Map from Paper entity to PaperMetadataDto
    @Mapping(target = "id", expression = "java(entity.getId() != null ? entity.getId().toString() : null)")
    @Mapping(target = "authors", source = "paperAuthors", qualifiedByName = "paperAuthorsToAuthorDtos")
    @Mapping(target = "externalIds", source = "externalIds", qualifiedByName = "externalIdsToMap")
    @Mapping(target = "venueName", source = "venue.venueName")
    @Mapping(target = "publisher", source = "venue.publisher")
    @Mapping(target = "volume", source = "venue.volume")
    @Mapping(target = "issue", source = "venue.issue")
    @Mapping(target = "pages", source = "venue.pages")
    @Mapping(target = "citationCount", source = "metrics.citationCount")
    @Mapping(target = "referenceCount", source = "metrics.referenceCount")
    @Mapping(target = "influentialCitationCount", source = "metrics.influentialCitationCount")
    @Mapping(target = "publicationTypes", source = "publicationTypes", qualifiedByName = "stringToList")
    @Mapping(target = "fieldsOfStudy", source = "fieldsOfStudy", qualifiedByName = "stringToList")
    PaperMetadataDto toMetadataDto(Paper entity);

    // Map from CreatePaperDto to Paper entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "paperAuthors", source = "authors", qualifiedByName = "authorsToPaperAuthors")
    @Mapping(target = "externalIds", source = "externalIds", qualifiedByName = "externalIdsMapToEntities")
    @Mapping(target = "venue", ignore = true)
    @Mapping(target = "metrics", ignore = true)
    @Mapping(target = "publicationTypes", source = "publicationTypes", qualifiedByName = "listToString")
    @Mapping(target = "fieldsOfStudy", source = "fieldsOfStudy", qualifiedByName = "listToString")
    Paper fromCreateDto(CreatePaperDto dto);

    // Map from UpdatePaperDto to Paper entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "correlationId", ignore = true)
    @Mapping(target = "paperAuthors", source = "authors", qualifiedByName = "authorsToPaperAuthors")
    @Mapping(target = "externalIds", source = "externalIds", qualifiedByName = "externalIdsMapToEntities")
    @Mapping(target = "venue", ignore = true)
    @Mapping(target = "metrics", ignore = true)
    @Mapping(target = "publicationTypes", source = "publicationTypes", qualifiedByName = "listToString")
    @Mapping(target = "fieldsOfStudy", source = "fieldsOfStudy", qualifiedByName = "listToString")
    Paper fromUpdateDto(UpdatePaperDto dto);

    // Map from Paper entity to PaperDto
    @Mapping(target = "authors", source = "paperAuthors", qualifiedByName = "paperAuthorsToAuthorDtos")
    @Mapping(target = "externalIds", source = "externalIds", qualifiedByName = "externalIdsToMap")
    @Mapping(target = "publicationTypes", source = "publicationTypes", qualifiedByName = "stringToList")
    @Mapping(target = "fieldsOfStudy", source = "fieldsOfStudy", qualifiedByName = "stringToList")
    PaperDto toDto(Paper entity);

    // Map from Paper entity list to PaperDto list
    List<PaperDto> toDtoList(List<Paper> entities);

    // Map from PaperMetadataDto list to Paper entity list
    List<Paper> fromMetadataDtoList(List<PaperMetadataDto> dtos);

    // Helper methods for string/list conversion
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

    // Helper methods for author mapping
    @Named("authorsToPaperAuthors")
    default List<org.solace.scholar_ai.project_service.model.paper.PaperAuthor> authorsToPaperAuthors(
            List<org.solace.scholar_ai.project_service.dto.author.AuthorDto> authors) {
        if (authors == null) return null;
        return authors.stream()
                .map(authorDto -> {
                    org.solace.scholar_ai.project_service.model.author.Author author =
                            AuthorMapper.INSTANCE.toEntity(authorDto);
                    return new org.solace.scholar_ai.project_service.model.paper.PaperAuthor(null, author);
                })
                .collect(Collectors.toList());
    }

    @Named("paperAuthorsToAuthorDtos")
    default List<org.solace.scholar_ai.project_service.dto.author.AuthorDto> paperAuthorsToAuthorDtos(
            List<org.solace.scholar_ai.project_service.model.paper.PaperAuthor> paperAuthors) {
        if (paperAuthors == null) return null;
        return paperAuthors.stream()
                .map(pa -> AuthorMapper.INSTANCE.toDto(pa.getAuthor()))
                .collect(Collectors.toList());
    }

    // Helper methods for external IDs mapping
    @Named("externalIdsMapToEntities")
    default List<org.solace.scholar_ai.project_service.model.paper.ExternalId> externalIdsMapToEntities(
            Map<String, Object> externalIdsMap) {
        if (externalIdsMap == null) return null;
        return externalIdsMap.entrySet().stream()
                .map(entry -> org.solace.scholar_ai.project_service.model.paper.ExternalId.builder()
                        .source(entry.getKey())
                        .value(entry.getValue().toString())
                        .build())
                .collect(Collectors.toList());
    }

    @Named("externalIdsToMap")
    default Map<String, Object> externalIdsToMap(
            List<org.solace.scholar_ai.project_service.model.paper.ExternalId> externalIds) {
        if (externalIds == null) return null;
        return externalIds.stream()
                .collect(Collectors.toMap(
                        org.solace.scholar_ai.project_service.model.paper.ExternalId::getSource,
                        org.solace.scholar_ai.project_service.model.paper.ExternalId::getValue));
    }

    // Helper methods for venue mapping
    @Named("mapVenue")
    default org.solace.scholar_ai.project_service.model.paper.PublicationVenue mapVenue(PaperMetadataDto dto) {
        if (dto.venueName() == null
                && dto.publisher() == null
                && dto.volume() == null
                && dto.issue() == null
                && dto.pages() == null) {
            return null;
        }
        return org.solace.scholar_ai.project_service.model.paper.PublicationVenue.builder()
                .venueName(dto.venueName())
                .publisher(dto.publisher())
                .volume(dto.volume())
                .issue(dto.issue())
                .pages(dto.pages())
                .build();
    }

    // Helper methods for metrics mapping
    @Named("mapMetrics")
    default org.solace.scholar_ai.project_service.model.paper.PaperMetrics mapMetrics(PaperMetadataDto dto) {
        if (dto.citationCount() == null && dto.referenceCount() == null && dto.influentialCitationCount() == null) {
            return null;
        }
        return org.solace.scholar_ai.project_service.model.paper.PaperMetrics.builder()
                .citationCount(dto.citationCount())
                .referenceCount(dto.referenceCount())
                .influentialCitationCount(dto.influentialCitationCount())
                .build();
    }
}
