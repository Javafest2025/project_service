package org.solace.scholar_ai.project_service.mapping.latex;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.solace.scholar_ai.project_service.dto.latex.CreateDocumentRequestDTO;
import org.solace.scholar_ai.project_service.dto.latex.DocumentResponseDTO;
import org.solace.scholar_ai.project_service.model.latex.Document;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DocumentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "filePath", ignore = true)
    Document toEntity(CreateDocumentRequestDTO dto);

    DocumentResponseDTO toResponseDTO(Document entity);

    List<DocumentResponseDTO> toResponseDTOList(List<Document> entities);
}
