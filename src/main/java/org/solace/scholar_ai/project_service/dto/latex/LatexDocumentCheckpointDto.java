package org.solace.scholar_ai.project_service.dto.latex;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LatexDocumentCheckpointDto {
    private Long id;
    private UUID documentId;
    private UUID sessionId;
    private Long messageId;
    private String checkpointName;
    private String contentBefore;
    private String contentAfter;
    private LocalDateTime createdAt;
    private Boolean isCurrent;

    // Helper properties
    private String displayName;
    private Long contentSizeDifference;
    private Boolean hasContentAfter;

    public String getDisplayName() {
        return String.format(
                "%s (%s)",
                checkpointName, createdAt != null ? createdAt.toString().substring(0, 16) : "");
    }

    public Long getContentSizeDifference() {
        if (contentAfter == null || contentBefore == null) return 0L;
        return (long) (contentAfter.length() - contentBefore.length());
    }

    public Boolean getHasContentAfter() {
        return contentAfter != null && !contentAfter.trim().isEmpty();
    }
}
