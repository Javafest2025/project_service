package org.solace.scholar_ai.project_service.dto.summary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.model.summary.PaperSummary;

/**
 * DTO for API responses to avoid circular references during JSON serialization
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class PaperSummaryResponseDto {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
     * Utility method to safely get a string value from a map
     */
    private static String getStringValue(Map<String, Object> map, String key) {
        if (map == null || key == null) {
            return null;
        }
        Object value = map.get(key);
        if (value instanceof String) {
            String str = (String) value;
            return str.trim().isEmpty() ? null : str;
        }
        return value != null ? value.toString() : null;
    }

    /**
     * Utility method to safely get a list value from a map
     */
    @SuppressWarnings("unchecked")
    private static List<String> getStringListValue(Map<String, Object> map, String key) {
        if (map == null || key == null) {
            return null;
        }
        Object value = map.get(key);
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return list.stream()
                    .filter(item -> item != null)
                    .map(Object::toString)
                    .filter(str -> !str.trim().isEmpty())
                    .toList();
        }
        return null;
    }

    /**
     * Parse JSON string to List<String> using Jackson
     */
    @SuppressWarnings("unchecked")
    private static List<String> parseJsonArray(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }
        try {
            if (jsonString.startsWith("[") && jsonString.endsWith("]")) {
                // Parse as JSON array
                List<Object> parsed = OBJECT_MAPPER.readValue(jsonString, List.class);
                return parsed.stream()
                        .filter(obj -> obj != null)
                        .map(Object::toString)
                        .filter(str -> !str.trim().isEmpty())
                        .distinct() // Remove duplicates
                        .toList();
            } else {
                // Single item, return as list if it's not empty
                String trimmed = jsonString.trim();
                return trimmed.isEmpty() ? List.of() : List.of(trimmed);
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse JSON array: {}", jsonString, e);
            // Fallback: return as single item list if it's not empty
            String trimmed = jsonString.trim();
            return trimmed.isEmpty() ? List.of() : List.of(trimmed);
        } catch (Exception e) {
            log.error("Unexpected error parsing JSON array: {}", jsonString, e);
            return List.of();
        }
    }

    /**
     * Parse JSON string to Map<String, Object> using Jackson
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseJsonObject(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }
        try {
            if (jsonString.startsWith("{") && jsonString.endsWith("}")) {
                // Parse as JSON object
                Map<String, Object> parsed = OBJECT_MAPPER.readValue(jsonString, Map.class);
                // Filter out null values and empty strings
                return parsed.entrySet().stream()
                        .filter(entry -> entry.getValue() != null)
                        .filter(entry -> !(entry.getValue() instanceof String)
                                || !((String) entry.getValue()).trim().isEmpty())
                        .collect(java.util.stream.Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (existing, replacement) -> existing,
                                java.util.LinkedHashMap::new));
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse JSON object: {}", jsonString, e);
        } catch (Exception e) {
            log.error("Unexpected error parsing JSON object: {}", jsonString, e);
        }
        return null;
    }

    /**
     * Parse JSON string to List<Map<String, Object>> using Jackson
     */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> parseJsonArrayToMap(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }
        try {
            if (jsonString.startsWith("[") && jsonString.endsWith("]")) {
                // Parse as JSON array of objects
                List<Map<String, Object>> parsed = OBJECT_MAPPER.readValue(jsonString, List.class);
                // Filter out null maps and maps with only null/empty values
                return parsed.stream()
                        .filter(map -> map != null)
                        .map(map -> map.entrySet().stream()
                                .filter(entry -> entry.getValue() != null)
                                .filter(entry -> !(entry.getValue() instanceof String)
                                        || !((String) entry.getValue()).trim().isEmpty())
                                .collect(java.util.stream.Collectors.toMap(
                                        Map.Entry::getKey,
                                        Map.Entry::getValue,
                                        (existing, replacement) -> existing,
                                        java.util.HashMap::new)))
                        .filter(map -> !map.isEmpty())
                        .<Map<String, Object>>map(map -> (Map<String, Object>) map)
                        .toList();
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse JSON array to map: {}", jsonString, e);
        } catch (Exception e) {
            log.error("Unexpected error parsing JSON array to map: {}", jsonString, e);
        }
        return null;
    }
}
