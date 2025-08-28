package org.solace.scholar_ai.project_service.dto.latex;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVersionDTO {
    private UUID id;
    private UUID documentId;
    private Integer versionNumber;
    private String content;
    private String commitMessage;
    private UUID createdBy;
    private Instant createdAt;
    private Boolean isAutoSave;
}
