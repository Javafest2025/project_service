package org.solace.scholar_ai.project_service.mapping.note;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.solace.scholar_ai.project_service.dto.author.AuthorDto;
import org.solace.scholar_ai.project_service.dto.note.PaperSuggestionDto;
import org.solace.scholar_ai.project_service.dto.paper.PaperMetadataDto;

@Mapper(componentModel = "spring")
public interface PaperMentionMapper {

    PaperMentionMapper INSTANCE = Mappers.getMapper(PaperMentionMapper.class);

    @Mapping(target = "displayText", expression = "java(generateDisplayText(dto))")
    @Mapping(target = "authors", source = "authors")
    PaperSuggestionDto toSuggestionDto(PaperMetadataDto dto);

    default String generateDisplayText(PaperMetadataDto dto) {
        StringBuilder displayText = new StringBuilder();

        // Add title (truncated if too long)
        String title = dto.title();
        if (title != null) {
            if (title.length() > 50) {
                title = title.substring(0, 47) + "...";
            }
            displayText.append(title);
        }

        // Add year if available
        if (dto.publicationDate() != null) {
            displayText.append(" (").append(dto.publicationDate().getYear()).append(")");
        }

        return displayText.toString();
    }

    /**
     * Custom mapping method to convert List<AuthorDto> to List<String>
     */
    default List<String> mapAuthors(List<AuthorDto> authors) {
        if (authors == null) {
            return null;
        }
        return authors.stream().map(AuthorDto::name).toList();
    }
}
