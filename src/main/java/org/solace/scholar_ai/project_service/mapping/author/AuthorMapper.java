package org.solace.scholar_ai.project_service.mapping.author;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.solace.scholar_ai.project_service.dto.author.AuthorDto;
import org.solace.scholar_ai.project_service.model.author.Author;

@Mapper(componentModel = "spring")
public interface AuthorMapper {

    AuthorMapper INSTANCE = Mappers.getMapper(AuthorMapper.class);

    AuthorDto toDto(Author author);

    Author toEntity(AuthorDto authorDto);
}
