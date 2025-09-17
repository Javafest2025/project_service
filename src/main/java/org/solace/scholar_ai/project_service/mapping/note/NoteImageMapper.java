package org.solace.scholar_ai.project_service.mapping.note;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.solace.scholar_ai.project_service.dto.note.ImageUploadDto;
import org.solace.scholar_ai.project_service.model.note.NoteImage;

@Mapper(componentModel = "spring")
public interface NoteImageMapper {

    NoteImageMapper INSTANCE = Mappers.getMapper(NoteImageMapper.class);

    @Mapping(target = "imageId", source = "entity.id")
    @Mapping(target = "imageUrl", source = "imageUrl")
    ImageUploadDto toDto(NoteImage entity, String imageUrl);
}
