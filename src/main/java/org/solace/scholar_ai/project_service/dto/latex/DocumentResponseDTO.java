package org.solace.scholar_ai.project_service.dto.latex;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.solace.scholar_ai.project_service.model.latex.DocumentType;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentResponseDTO {
    private UUID id;
    private UUID projectId;
    private String title;
    private String content;
    private DocumentType documentType;
    private String filePath;
    private Instant createdAt;
    private Instant updatedAt;
}
