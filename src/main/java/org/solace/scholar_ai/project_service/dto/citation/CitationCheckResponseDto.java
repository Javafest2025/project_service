package org.solace.scholar_ai.project_service.dto.citation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitationCheckResponseDto {

    private UUID id;

    private UUID projectId;

    private UUID documentId;

    private String status; // QUEUED, RUNNING, DONE, ERROR

    private String currentStep; // Current processing step

    private Integer progressPercent; // 0-100

    private String message; // Status message or error

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;

    private Map<String, Object> summary; // JSON summary of results

    private List<CitationIssueDto> issues; // Issues found (if completed)

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CitationIssueDto {

        private UUID id;

        private String issueType; // missing-citation, weak-citation, etc.

        private String severity; // ERROR, WARNING, INFO

        private String citationText; // The citation or claim text

        private Integer position; // Character position in document

        private Integer length; // Length of the problematic text

        private Integer lineStart; // Starting line number

        private Integer lineEnd; // Ending line number

        private String message; // Human-readable issue description

        private String suggestion; // Suggested fix or improvement

        private Boolean resolved; // Whether user marked as resolved

        private List<EvidenceDto> evidence; // Supporting/contradicting evidence

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class EvidenceDto {

            private UUID id;

            private Map<String, Object> source; // JSON source information

            private String matchedText; // Text that matched

            private Double similarity; // 0-1 similarity score

            private Double supportScore; // 0-1 support confidence

            private String extractedContext; // Surrounding context
        }
    }
}
