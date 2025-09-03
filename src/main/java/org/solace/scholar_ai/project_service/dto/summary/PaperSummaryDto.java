package org.solace.scholar_ai.project_service.dto.summary;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Main DTO for paper summary containing all structured fields
 */
@Data
@Builder
public class PaperSummaryDto {

    // Quick Take
    @JsonProperty("one_liner")
    private String oneLiner;

    @JsonProperty("key_contributions")
    private List<String> keyContributions;

    @JsonProperty("method_overview")
    private String methodOverview;

    @JsonProperty("main_findings")
    private List<Finding> mainFindings;

    @JsonProperty("limitations")
    private List<String> limitations;

    @JsonProperty("applicability")
    private List<String> applicability;

    // Methods & Data
    @JsonProperty("study_type")
    private String studyType;

    @JsonProperty("research_questions")
    private List<String> researchQuestions;

    @JsonProperty("datasets")
    private List<DatasetInfo> datasets;

    @JsonProperty("participants")
    private ParticipantInfo participants;

    @JsonProperty("procedure_or_pipeline")
    private String procedureOrPipeline;

    @JsonProperty("baselines_or_controls")
    private List<String> baselinesOrControls;

    @JsonProperty("metrics")
    private List<MetricInfo> metrics;

    @JsonProperty("statistical_analysis")
    private List<String> statisticalAnalysis;

    @JsonProperty("compute_resources")
    private ComputeInfo computeResources;

    @JsonProperty("implementation_details")
    private ImplementationInfo implementationDetails;

    // Reproducibility
    @JsonProperty("artifacts")
    private ArtifactInfo artifacts;

    @JsonProperty("reproducibility_notes")
    private String reproducibilityNotes;

    @JsonProperty("repro_score")
    private Double reproScore;

    // Ethics & Compliance
    @JsonProperty("ethics")
    private EthicsInfo ethics;

    @JsonProperty("bias_and_fairness")
    private List<String> biasAndFairness;

    @JsonProperty("risks_and_misuse")
    private List<String> risksAndMisuse;

    @JsonProperty("data_rights")
    private String dataRights;

    // Context & Impact
    @JsonProperty("novelty_type")
    private String noveltyType;

    @JsonProperty("positioning")
    private List<String> positioning;

    @JsonProperty("related_works_key")
    private List<RelatedWork> relatedWorksKey;

    @JsonProperty("impact_notes")
    private String impactNotes;

    // Quality & Trust
    @JsonProperty("confidence")
    private Double confidence;

    @JsonProperty("evidence_anchors")
    private List<EvidenceAnchor> evidenceAnchors;

    @JsonProperty("threats_to_validity")
    private List<String> threatsToValidity;

    // Additional fields
    @JsonProperty("domain_classification")
    private List<String> domainClassification;

    @JsonProperty("technical_depth")
    private String technicalDepth;

    @JsonProperty("interdisciplinary_connections")
    private List<String> interdisciplinaryConnections;

    @JsonProperty("future_work")
    private List<String> futureWork;

    // Nested DTOs
    @Data
    @Builder
    public static class Finding {
        private String task;
        private String metric;
        private String value;
        private String comparator;
        private String delta;
        private String significance;
    }

    @Data
    @Builder
    public static class DatasetInfo {
        private String name;
        private String domain;
        private String size;
        private String splitInfo;
        private String license;
        private String url;
        private String description;
    }

    @Data
    @Builder
    public static class ParticipantInfo {
        private Integer n;
        private String demographics;
        private Boolean irbApproved;
        private String recruitmentMethod;
        private String compensationDetails;
    }

    @Data
    @Builder
    public static class MetricInfo {
        private String name;
        private String definition;
        private String formula;
        private String interpretation;
    }

    @Data
    @Builder
    public static class ComputeInfo {
        private String hardware;
        private String trainingTime;
        private Double energyEstimateKwh;
        private String cloudProvider;
        private Double estimatedCost;
        private Integer gpuCount;
    }

    @Data
    @Builder
    public static class ImplementationInfo {
        private List<String> frameworks;
        private Map<String, Object> keyHyperparams;
        private String language;
        private String dependencies;
        private Integer codeLines;
    }

    @Data
    @Builder
    public static class ArtifactInfo {
        private String codeUrl;
        private String dataUrl;
        private String modelUrl;
        private String dockerImage;
        private String configFiles;
        private String demoUrl;
        private String supplementaryMaterial;
    }

    @Data
    @Builder
    public static class EthicsInfo {
        private Boolean irb;
        private Boolean consent;
        private Boolean sensitiveData;
        private String privacyMeasures;
        private String dataAnonymization;
    }

    @Data
    @Builder
    public static class RelatedWork {
        private String citation;
        private String relation; // supports|contradicts|builds_on|extends|competes
        private String description;
        private String year;
    }

    @Data
    @Builder
    public static class EvidenceAnchor {
        private String field;
        private Integer page;
        private String span;
        private String source; // section|figure|table|equation|reference
        private String sourceId;
        private Double confidence;
    }
}
