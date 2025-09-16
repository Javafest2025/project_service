package org.solace.scholar_ai.project_service.dto.citation;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitationCheckRequestDto {

    private UUID projectId;

    private UUID documentId;

    private String content; // LaTeX content to check

    private String filename; // Optional filename for context

    private Boolean forceRecheck; // Force recheck even if recent check exists

    private CitationCheckOptionsDto options;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CitationCheckOptionsDto {

        private Boolean checkLocal; // Check against local papers

        private Boolean checkWeb; // Check against web sources

        private Double similarityThreshold; // Minimum similarity for evidence

        private Integer maxEvidencePerIssue; // Max evidence items per issue

        private Boolean enablePlagiarismCheck; // Check for potential plagiarism

        private Boolean strictMode; // More stringent checking
    }
}
