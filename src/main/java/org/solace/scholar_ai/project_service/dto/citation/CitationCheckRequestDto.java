package org.solace.scholar_ai.project_service.dto.citation;

import java.util.List;
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

    private List<UUID> selectedPaperIds; // NEW - specific papers to use for local corpus

    private String content; // LaTeX content to check

    private String filename; // Optional filename for context

    private String contentHash; // NEW - SHA256 hash of content for caching

    private Boolean forceRecheck; // Force recheck even if recent check exists

    private Options options;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Options {

        @Builder.Default
        private Boolean checkLocal = true; // Check against local papers

        @Builder.Default
        private Boolean checkWeb = true; // Check against web sources

        @Builder.Default
        private Double similarityThreshold = 0.85; // Minimum similarity for evidence

        @Builder.Default
        private Double plagiarismThreshold = 0.92; // Minimum similarity for plagiarism detection

        @Builder.Default
        private Integer maxEvidencePerIssue = 5; // Max evidence items per issue

        @Builder.Default
        private Boolean strictMode = true; // More stringent checking
    }
}
