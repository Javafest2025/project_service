package org.solace.scholar_ai.project_service.dto.latex;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateDocumentRequestDTO {

    @NotNull(message = "Document ID is required") private UUID documentId;

    private String title;
    private String content;
}
