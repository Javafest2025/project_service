package org.solace.scholar_ai.project_service.mapping.author;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.solace.scholar_ai.project_service.dto.author.AuthorDto;
import org.solace.scholar_ai.project_service.model.author.Author;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuthorMapper {

    AuthorMapper INSTANCE = Mappers.getMapper(AuthorMapper.class);

    @Mapping(target = "allAffiliations", source = "allAffiliations", qualifiedByName = "jsonStringToList")
    @Mapping(target = "researchAreas", source = "researchAreas", qualifiedByName = "jsonStringToList")
    @Mapping(target = "recentPublications", source = "recentPublications", qualifiedByName = "jsonStringToObjectList")
    @Mapping(target = "dataSources", source = "dataSources", qualifiedByName = "jsonStringToList")
    @Mapping(target = "sourcesAttempted", source = "sourcesAttempted", qualifiedByName = "jsonStringToList")
    @Mapping(target = "sourcesSuccessful", source = "sourcesSuccessful", qualifiedByName = "jsonStringToList")
    AuthorDto toDto(Author author);

    @Mapping(target = "allAffiliations", source = "allAffiliations", qualifiedByName = "listToJsonString")
    @Mapping(target = "researchAreas", source = "researchAreas", qualifiedByName = "listToJsonString")
    @Mapping(target = "recentPublications", source = "recentPublications", qualifiedByName = "objectListToJsonString")
    @Mapping(target = "dataSources", source = "dataSources", qualifiedByName = "listToJsonString")
    @Mapping(target = "sourcesAttempted", source = "sourcesAttempted", qualifiedByName = "listToJsonString")
    @Mapping(target = "sourcesSuccessful", source = "sourcesSuccessful", qualifiedByName = "listToJsonString")
    Author toEntity(AuthorDto authorDto);

    // Helper methods for JSON conversion (unique names to avoid conflicts with
    // PaperMapper)
    @Named("jsonStringToList")
    default List<String> jsonStringToList(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonString, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    @Named("jsonStringToObjectList")
    default List<Object> jsonStringToObjectList(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonString, new TypeReference<List<Object>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    @Named("jsonStringToObject")
    default Object jsonStringToObject(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonString, Object.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Named("listToJsonString")
    default String listToJsonString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Named("objectListToJsonString")
    default String objectListToJsonString(List<Object> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Named("objectToJsonString")
    default String objectToJsonString(Object object) {
        if (object == null) {
            return null;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
