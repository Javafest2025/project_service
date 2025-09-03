package org.solace.scholar_ai.project_service.dto.summary;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.solace.scholar_ai.project_service.model.summary.PaperSummary;

/**
 * DTO for API responses to avoid circular references during JSON serialization
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperSummaryResponseDto {

    private UUID id;
    private UUID paperId; // Just the ID, not the full Paper object

    // Quick Take Section
    private String oneLiner;
    private List<String> keyContributions; // JSON array
    private String methodOverview;
    private List<Map<String, Object>> mainFindings; // JSON array of findings objects
    private List<String> limitations; // JSON array
    private List<String> applicability; // JSON array

    // Methods & Data Section
    private String studyType;
    private List<String> researchQuestions; // JSON array
    private List<Map<String, Object>> datasets; // JSON array of dataset objects
    private Map<String, Object> participants; // JSON object
    private String procedureOrPipeline;
    private List<String> baselinesOrControls; // JSON array
    private List<Map<String, Object>> metrics; // JSON array with definitions
    private List<String> statisticalAnalysis; // JSON array
    private Map<String, Object> computeResources; // JSON object
    private Map<String, Object> implementationDetails; // JSON object

    // Reproducibility Section
    private Map<String, Object> artifacts; // JSON object with URLs
    private String reproducibilityNotes;
    private Double reproScore;

    // Ethics & Compliance Section
    private Map<String, Object> ethics; // JSON object
    private List<String> biasAndFairness; // JSON array
    private List<String> risksAndMisuse; // JSON array
    private String dataRights;

    // Context & Impact Section
    private String noveltyType;
    private List<String> positioning; // JSON array
    private List<Map<String, Object>> relatedWorksKey; // JSON array of citation objects
    private String impactNotes;

    // Quality & Trust Section
    private Double confidence;
    private List<Map<String, Object>> evidenceAnchors; // JSON array
    private List<String> threatsToValidity; // JSON array

    // Additional fields for enhanced tracking
    private List<String> domainClassification; // JSON array of domains
    private String technicalDepth; // introductory|intermediate|advanced|expert
    private List<String> interdisciplinaryConnections; // JSON array
    private List<String> futureWork; // JSON array

    // Generation metadata
    private String modelVersion;
    private String responseSource;
    private String fallbackReason;
    private Instant generationTimestamp;
    private Double generationTimeSeconds;
    private Integer promptTokens;
    private Integer completionTokens;
    private Double extractionCoverageUsed;

    // Validation
    private String validationStatus;
    private String validationNotes;

    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Convert from PaperSummary entity to DTO
     */
    public static PaperSummaryResponseDto fromEntity(PaperSummary summary) {
        if (summary == null) {
            return null;
        }

        return PaperSummaryResponseDto.builder()
                .id(summary.getId())
                .paperId(summary.getPaper() != null ? summary.getPaper().getId() : null)
                .oneLiner(summary.getOneLiner())
                .keyContributions(parseJsonArray(summary.getKeyContributions()))
                .methodOverview(summary.getMethodOverview())
                .mainFindings(parseJsonArrayToMap(summary.getMainFindings()))
                .limitations(parseJsonArray(summary.getLimitations()))
                .applicability(parseJsonArray(summary.getApplicability()))
                .studyType(
                        summary.getStudyType() != null ? summary.getStudyType().name() : null)
                .researchQuestions(parseJsonArray(summary.getResearchQuestions()))
                .datasets(parseJsonArrayToMap(summary.getDatasets()))
                .participants(parseJsonObject(summary.getParticipants()))
                .procedureOrPipeline(summary.getProcedureOrPipeline())
                .baselinesOrControls(parseJsonArray(summary.getBaselinesOrControls()))
                .metrics(parseJsonArrayToMap(summary.getMetrics()))
                .statisticalAnalysis(parseJsonArray(summary.getStatisticalAnalysis()))
                .computeResources(parseJsonObject(summary.getComputeResources()))
                .implementationDetails(parseJsonObject(summary.getImplementationDetails()))
                .artifacts(parseJsonObject(summary.getArtifacts()))
                .reproducibilityNotes(summary.getReproducibilityNotes())
                .reproScore(summary.getReproScore())
                .ethics(parseJsonObject(summary.getEthics()))
                .biasAndFairness(parseJsonArray(summary.getBiasAndFairness()))
                .risksAndMisuse(parseJsonArray(summary.getRisksAndMisuse()))
                .dataRights(summary.getDataRights())
                .noveltyType(
                        summary.getNoveltyType() != null
                                ? summary.getNoveltyType().name()
                                : null)
                .positioning(parseJsonArray(summary.getPositioning()))
                .relatedWorksKey(parseJsonArrayToMap(summary.getRelatedWorksKey()))
                .impactNotes(summary.getImpactNotes())
                .confidence(summary.getConfidence())
                .evidenceAnchors(parseJsonArrayToMap(summary.getEvidenceAnchors()))
                .threatsToValidity(parseJsonArray(summary.getThreatsToValidity()))
                .domainClassification(parseJsonArray(summary.getDomainClassification()))
                .technicalDepth(summary.getTechnicalDepth())
                .interdisciplinaryConnections(parseJsonArray(summary.getInterdisciplinaryConnections()))
                .futureWork(parseJsonArray(summary.getFutureWork()))
                .modelVersion(summary.getModelVersion())
                .responseSource(
                        summary.getResponseSource() != null
                                ? summary.getResponseSource().name()
                                : null)
                .fallbackReason(summary.getFallbackReason())
                .generationTimestamp(summary.getGenerationTimestamp())
                .generationTimeSeconds(summary.getGenerationTimeSeconds())
                .promptTokens(summary.getPromptTokens())
                .completionTokens(summary.getCompletionTokens())
                .extractionCoverageUsed(summary.getExtractionCoverageUsed())
                .validationStatus(
                        summary.getValidationStatus() != null
                                ? summary.getValidationStatus().name()
                                : null)
                .validationNotes(summary.getValidationNotes())
                .createdAt(summary.getCreatedAt())
                .updatedAt(summary.getUpdatedAt())
                .build();
    }

    /**
     * Parse JSON string to List<String>
     */
    @SuppressWarnings("unchecked")
    private static List<String> parseJsonArray(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }
        try {
            // Simple parsing for common JSON array patterns
            if (jsonString.startsWith("[") && jsonString.endsWith("]")) {
                // Remove brackets and split by comma, handling quotes
                String content = jsonString.substring(1, jsonString.length() - 1).trim();
                if (content.isEmpty()) {
                    return List.of();
                }

                // Split by comma, handling quoted strings
                List<String> result = new java.util.ArrayList<>();
                StringBuilder current = new StringBuilder();
                boolean inQuotes = false;
                boolean escaped = false;

                for (int i = 0; i < content.length(); i++) {
                    char c = content.charAt(i);
                    if (escaped) {
                        current.append(c);
                        escaped = false;
                    } else if (c == '\\') {
                        escaped = true;
                    } else if (c == '"') {
                        inQuotes = !inQuotes;
                    } else if (c == ',' && !inQuotes) {
                        String item = current.toString().trim();
                        if (!item.isEmpty()) {
                            // Remove surrounding quotes if present
                            if (item.startsWith("\"") && item.endsWith("\"")) {
                                item = item.substring(1, item.length() - 1);
                            }
                            result.add(item);
                        }
                        current.setLength(0);
                    } else {
                        current.append(c);
                    }
                }

                // Add the last item
                String lastItem = current.toString().trim();
                if (!lastItem.isEmpty()) {
                    if (lastItem.startsWith("\"") && lastItem.endsWith("\"")) {
                        lastItem = lastItem.substring(1, lastItem.length() - 1);
                    }
                    result.add(lastItem);
                }

                return result;
            }
        } catch (Exception e) {
            // If parsing fails, return as single item list
        }
        return List.of(jsonString);
    }

    /**
     * Parse JSON string to Map<String, Object>
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseJsonObject(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }
        try {
            // Simple parsing for common JSON object patterns
            if (jsonString.startsWith("{") && jsonString.endsWith("}")) {
                // For now, return a simple map with the raw JSON
                // In production, you might want to use a proper JSON parser
                return Map.of("rawJson", jsonString);
            }
        } catch (Exception e) {
            // If parsing fails, return null
        }
        return null;
    }

    /**
     * Parse JSON string to List<Map<String, Object>>
     */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> parseJsonArrayToMap(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }
        try {
            // Simple parsing for common JSON array patterns
            if (jsonString.startsWith("[") && jsonString.endsWith("]")) {
                // For now, return a list with the raw JSON
                // In production, you might want to use a proper JSON parser
                return List.of(Map.of("rawJson", jsonString));
            }
        } catch (Exception e) {
            // If parsing fails, return null
        }
        return null;
    }
}
