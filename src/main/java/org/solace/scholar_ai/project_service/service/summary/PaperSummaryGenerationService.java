package org.solace.scholar_ai.project_service.service.summary;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.summary.ExtractionContext;
import org.solace.scholar_ai.project_service.dto.summary.PaperSummaryDto;
import org.solace.scholar_ai.project_service.exception.PaperNotExtractedException;
import org.solace.scholar_ai.project_service.model.extraction.*;
import org.solace.scholar_ai.project_service.model.paper.Paper;
import org.solace.scholar_ai.project_service.model.summary.PaperSummary;
import org.solace.scholar_ai.project_service.repository.extraction.PaperExtractionRepository;
import org.solace.scholar_ai.project_service.repository.paper.PaperRepository;
import org.solace.scholar_ai.project_service.repository.summary.PaperSummaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for generating comprehensive paper summaries using extracted data and
 * Gemini AI
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaperSummaryGenerationService {

    private final PaperRepository paperRepository;
    private final PaperExtractionRepository extractionRepository;
    private final PaperSummaryRepository summaryRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    /**
     * Generate a comprehensive summary for a paper
     */
    @Transactional
    public PaperSummary generateSummary(UUID paperId) {
        log.info("Starting summary generation for paper: {}", paperId);
        long startTime = System.currentTimeMillis();

        try {
            // 1. Check if paper exists first
            Paper paper = paperRepository
                    .findById(paperId)
                    .orElseThrow(() -> new RuntimeException("Paper not found: " + paperId));

            // 2. Check if paper has been extracted
            if (!paper.getIsExtracted()) {
                throw new PaperNotExtractedException(
                        "Paper has not been extracted yet. Please extract the paper first before generating a summary.");
            }

            // 3. Check extraction status
            if (!"COMPLETED".equals(paper.getExtractionStatus())) {
                throw new PaperNotExtractedException("Paper extraction is not completed. Current status: "
                        + paper.getExtractionStatus() + ". Please wait for extraction to complete.");
            }

            // 4. Fetch extraction data
            PaperExtraction extraction = extractionRepository
                    .findByPaperId(paperId)
                    .orElseThrow(() -> new RuntimeException("No extraction found for paper: " + paperId));

            // 3. Build extraction context
            ExtractionContext context = buildExtractionContext(extraction);

            // 4. Generate summary using parallel processing for different sections
            PaperSummaryDto summaryDTO = generateSummaryWithGemini(context, extraction);

            // 5. Calculate quality metrics
            enrichSummaryWithMetrics(summaryDTO, context);

            // 6. Save to database
            PaperSummary summary = saveSummary(summaryDTO, extraction.getPaper(), startTime);

            log.info(
                    "Summary generation completed for paper: {} in {} seconds",
                    paperId,
                    (System.currentTimeMillis() - startTime) / 1000.0);

            return summary;

        } catch (PaperNotExtractedException e) {
            // Re-throw PaperNotExtractedException directly to preserve the exception type
            throw e;
        } catch (Exception e) {
            log.error("Error generating summary for paper: {}", paperId, e);
            throw new RuntimeException("Summary generation failed", e);
        }
    }

    /**
     * Build extraction context from all extracted data
     */
    private ExtractionContext buildExtractionContext(PaperExtraction extraction) {
        log.debug(
                "Building extraction context for paper: {}",
                extraction.getPaper().getId());

        // Process sections hierarchically
        List<ExtractionContext.SectionContent> sections = processSections(extraction.getSections());

        // Process figures
        List<ExtractionContext.FigureContent> figures = extraction.getFigures().stream().map(this::mapFigure)
                .collect(Collectors.toList());

        // Process tables
        List<ExtractionContext.TableContent> tables = extraction.getTables().stream().map(this::mapTable)
                .collect(Collectors.toList());

        // Process equations
        List<ExtractionContext.EquationContent> equations = extraction.getEquations().stream().map(this::mapEquation)
                .collect(Collectors.toList());

        // Process code blocks
        List<ExtractionContext.CodeBlockContent> codeBlocks = extraction.getCodeBlocks().stream()
                .map(this::mapCodeBlock).collect(Collectors.toList());

        // Process references
        List<ExtractionContext.ReferenceContent> references = extraction.getReferences().stream()
                .map(this::mapReference).collect(Collectors.toList());

        // Process entities
        List<ExtractionContext.EntityContent> entities = extraction.getEntities().stream().map(this::mapEntity)
                .collect(Collectors.toList());

        return ExtractionContext.builder()
                .title(extraction.getTitle())
                .abstractText(extraction.getAbstractText())
                .sections(sections)
                .figures(figures)
                .tables(tables)
                .equations(equations)
                .codeBlocks(codeBlocks)
                .references(references)
                .entities(entities)
                .pageCount(extraction.getPageCount())
                .language(extraction.getLanguage())
                .build();
    }

    /**
     * Generate summary using Gemini with parallel processing
     */
    private PaperSummaryDto generateSummaryWithGemini(ExtractionContext context, PaperExtraction extraction) {
        log.debug("Generating summary with Gemini for paper: {}", extraction.getTitle());

        // Create parallel tasks for different aspects
        CompletableFuture<Map<String, Object>> quickTakeFuture = CompletableFuture
                .supplyAsync(() -> generateQuickTake(context), executorService);

        CompletableFuture<Map<String, Object>> methodsFuture = CompletableFuture
                .supplyAsync(() -> generateMethodsAndData(context), executorService);

        CompletableFuture<Map<String, Object>> reproducibilityFuture = CompletableFuture
                .supplyAsync(() -> generateReproducibility(context), executorService);

        CompletableFuture<Map<String, Object>> ethicsFuture = CompletableFuture
                .supplyAsync(() -> generateEthicsAndCompliance(context), executorService);

        CompletableFuture<Map<String, Object>> contextImpactFuture = CompletableFuture
                .supplyAsync(() -> generateContextAndImpact(context), executorService);

        // Wait for all tasks and combine results
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                quickTakeFuture, methodsFuture, reproducibilityFuture, ethicsFuture, contextImpactFuture);

        allFutures.join();

        // Combine results
        Map<String, Object> quickTake = quickTakeFuture.join();
        Map<String, Object> methods = methodsFuture.join();
        Map<String, Object> reproducibility = reproducibilityFuture.join();
        Map<String, Object> ethics = ethicsFuture.join();
        Map<String, Object> contextImpact = contextImpactFuture.join();

        // Build final summary DTO
        return buildSummaryDTO(quickTake, methods, reproducibility, ethics, contextImpact, context);
    }

    /**
     * Generate Quick Take section using Gemini
     */
    private Map<String, Object> generateQuickTake(ExtractionContext context) {
        String prompt = PromptBuilder.buildQuickTakePrompt(context);
        String response = geminiService.generate(
                prompt,
                GeminiService.GenerationConfig.builder()
                        .temperature(0.3)
                        .maxOutputTokens(1500)
                        .build());

        return parseJsonResponse(response);
    }

    /**
     * Generate Methods and Data section using Gemini
     */
    private Map<String, Object> generateMethodsAndData(ExtractionContext context) {
        String prompt = PromptBuilder.buildMethodsPrompt(context);
        String response = geminiService.generate(
                prompt,
                GeminiService.GenerationConfig.builder()
                        .temperature(0.2)
                        .maxOutputTokens(2000)
                        .build());

        return parseJsonResponse(response);
    }

    /**
     * Generate Reproducibility section using Gemini
     */
    private Map<String, Object> generateReproducibility(ExtractionContext context) {
        String prompt = PromptBuilder.buildReproducibilityPrompt(context);
        String response = geminiService.generate(
                prompt,
                GeminiService.GenerationConfig.builder()
                        .temperature(0.2)
                        .maxOutputTokens(1000)
                        .build());

        return parseJsonResponse(response);
    }

    /**
     * Generate Ethics and Compliance section using Gemini
     */
    private Map<String, Object> generateEthicsAndCompliance(ExtractionContext context) {
        String prompt = PromptBuilder.buildEthicsPrompt(context);
        String response = geminiService.generate(
                prompt,
                GeminiService.GenerationConfig.builder()
                        .temperature(0.3)
                        .maxOutputTokens(1000)
                        .build());

        return parseJsonResponse(response);
    }

    /**
     * Generate Context and Impact section using Gemini
     */
    private Map<String, Object> generateContextAndImpact(ExtractionContext context) {
        String prompt = PromptBuilder.buildContextImpactPrompt(context);
        String response = geminiService.generate(
                prompt,
                GeminiService.GenerationConfig.builder()
                        .temperature(0.4)
                        .maxOutputTokens(1500)
                        .build());

        return parseJsonResponse(response);
    }

    /**
     * Process sections hierarchically
     */
    private List<ExtractionContext.SectionContent> processSections(List<ExtractedSection> sections) {
        // Sort by order index
        sections.sort(
                Comparator.comparing(ExtractedSection::getOrderIndex, Comparator.nullsLast(Comparator.naturalOrder())));

        List<ExtractionContext.SectionContent> result = new ArrayList<>();

        for (ExtractedSection section : sections) {
            // Only process top-level sections (parent is null)
            if (section.getParentSection() == null) {
                result.add(processSectionRecursively(section));
            }
        }

        return result;
    }

    /**
     * Process a section and its subsections recursively
     */
    private ExtractionContext.SectionContent processSectionRecursively(ExtractedSection section) {
        List<String> paragraphs = section.getParagraphs().stream()
                .sorted(Comparator.comparing(
                        ExtractedParagraph::getOrderIndex, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(ExtractedParagraph::getText)
                .collect(Collectors.toList());

        // Add subsection content
        for (ExtractedSection subsection : section.getSubsections()) {
            ExtractionContext.SectionContent subContent = processSectionRecursively(subsection);
            paragraphs.addAll(subContent.getParagraphs());
        }

        return ExtractionContext.SectionContent.builder()
                .id(section.getSectionId())
                .title(section.getTitle())
                .type(section.getSectionType())
                .level(section.getLevel())
                .paragraphs(paragraphs)
                .pageStart(section.getPageStart())
                .pageEnd(section.getPageEnd())
                .build();
    }

    // Mapping methods for other entities
    private ExtractionContext.FigureContent mapFigure(ExtractedFigure figure) {
        return ExtractionContext.FigureContent.builder()
                .id(figure.getFigureId())
                .label(figure.getLabel())
                .caption(figure.getCaption())
                .page(figure.getPage())
                .ocrText(figure.getOcrText())
                .build();
    }

    private ExtractionContext.TableContent mapTable(ExtractedTable table) {
        return ExtractionContext.TableContent.builder()
                .id(table.getTableId())
                .label(table.getLabel())
                .caption(table.getCaption())
                .page(table.getPage())
                .headers(table.getHeaders())
                .rows(table.getRows())
                .html(table.getHtml())
                .build();
    }

    private ExtractionContext.EquationContent mapEquation(ExtractedEquation equation) {
        return ExtractionContext.EquationContent.builder()
                .id(equation.getEquationId())
                .label(equation.getLabel())
                .latexContent(equation.getLatex())
                .page(equation.getPage())
                .build();
    }

    private ExtractionContext.CodeBlockContent mapCodeBlock(ExtractedCodeBlock codeBlock) {
        return ExtractionContext.CodeBlockContent.builder()
                .id(codeBlock.getCodeId())
                .language(codeBlock.getLanguage())
                .code(codeBlock.getCode())
                .page(codeBlock.getPage())
                .build();
    }

    private ExtractionContext.ReferenceContent mapReference(ExtractedReference reference) {
        return ExtractionContext.ReferenceContent.builder()
                .id(reference.getReferenceId())
                .title(reference.getTitle())
                .authors(reference.getAuthors())
                .year(reference.getYear())
                .venue(reference.getVenue())
                .doi(reference.getDoi())
                .build();
    }

    private ExtractionContext.EntityContent mapEntity(ExtractedEntity entity) {
        return ExtractionContext.EntityContent.builder()
                .id(entity.getEntityId())
                .text(entity.getName())
                .type(entity.getEntityType())
                .metadata(entity.getContext())
                .build();
    }

    /**
     * Parse JSON response from Gemini and detect response source
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonResponse(String response) {
        try {
            // Extract JSON from response (Gemini might include markdown)
            String json = response;
            if (response.contains("```json")) {
                int start = response.indexOf("```json") + 7;
                int end = response.lastIndexOf("```");
                json = response.substring(start, end).trim();
            }

            Map<String, Object> parsed = objectMapper.readValue(json, Map.class);

            // Check if this is a fallback response
            if (parsed.containsKey("_response_source") && "fallback".equals(parsed.get("_response_source"))) {
                log.info("Detected fallback response: {}", parsed.get("_fallback_reason"));
                isFallbackResponse = true;
                fallbackReason = (String) parsed.get("_fallback_reason");
                // Remove metadata fields from the parsed response
                parsed.remove("_response_source");
                parsed.remove("_fallback_reason");
                parsed.remove("_timestamp");
            } else {
                isFallbackResponse = false;
                fallbackReason = null;
            }

            return parsed;
        } catch (Exception e) {
            log.error("Failed to parse JSON response: {}", response, e);
            return new HashMap<>();
        }
    }

    /**
     * Track response source for monitoring and debugging
     */
    private boolean isFallbackResponse = false;

    private String fallbackReason = null;

    /**
     * Build final summary DTO from all components
     */
    private PaperSummaryDto buildSummaryDTO(
            Map<String, Object> quickTake,
            Map<String, Object> methods,
            Map<String, Object> reproducibility,
            Map<String, Object> ethics,
            Map<String, Object> contextImpact,
            ExtractionContext context) {

        PaperSummaryDto.PaperSummaryDtoBuilder builder = PaperSummaryDto.builder();

        // Quick Take
        builder.oneLiner((String) quickTake.get("one_liner"));
        builder.keyContributions(convertToList(quickTake.get("key_contributions")));
        builder.methodOverview((String) quickTake.get("method_overview"));
        builder.mainFindings(convertToFindings(quickTake.get("main_findings")));
        builder.limitations(convertToList(quickTake.get("limitations")));
        builder.applicability(convertToList(quickTake.get("applicability")));

        // Methods & Data
        builder.studyType((String) methods.get("study_type"));
        builder.researchQuestions(convertToList(methods.get("research_questions")));
        builder.datasets(convertToDatasets(methods.get("datasets")));
        builder.participants(convertToParticipants(methods.get("participants")));
        builder.procedureOrPipeline((String) methods.get("procedure_or_pipeline"));
        builder.baselinesOrControls(convertToList(methods.get("baselines_or_controls")));
        builder.metrics(convertToMetrics(methods.get("metrics")));
        builder.statisticalAnalysis(convertToList(methods.get("statistical_analysis")));
        builder.computeResources(convertToCompute(methods.get("compute_resources")));
        builder.implementationDetails(convertToImplementation(methods.get("implementation_details")));

        // Reproducibility
        builder.artifacts(convertToArtifacts(reproducibility.get("artifacts")));
        builder.reproducibilityNotes((String) reproducibility.get("reproducibility_notes"));
        builder.reproScore(convertToDouble(reproducibility.get("repro_score")));

        // Ethics & Compliance
        builder.ethics(convertToEthics(ethics.get("ethics")));
        builder.biasAndFairness(convertToList(ethics.get("bias_and_fairness")));
        builder.risksAndMisuse(convertToList(ethics.get("risks_and_misuse")));
        builder.dataRights((String) ethics.get("data_rights"));

        // Context & Impact
        builder.noveltyType((String) contextImpact.get("novelty_type"));
        builder.positioning(convertToList(contextImpact.get("positioning")));
        builder.relatedWorksKey(convertToRelatedWorks(contextImpact.get("related_works_key")));
        builder.impactNotes((String) contextImpact.get("impact_notes"));

        // Additional fields
        builder.domainClassification(convertToList(contextImpact.get("domain_classification")));
        builder.technicalDepth((String) contextImpact.get("technical_depth"));
        builder.interdisciplinaryConnections(convertToList(contextImpact.get("interdisciplinary_connections")));
        builder.futureWork(convertToList(contextImpact.get("future_work")));

        // Evidence anchors are generated separately
        builder.evidenceAnchors(generateEvidenceAnchors(context));
        builder.threatsToValidity(convertToList(contextImpact.get("threats_to_validity")));

        // Calculate confidence based on extraction coverage and response quality
        builder.confidence(calculateConfidence(quickTake, methods, reproducibility, ethics, contextImpact));

        return builder.build();
    }

    /**
     * Calculate confidence score based on extraction completeness
     */
    private Double calculateConfidence(Map<String, Object>... sections) {
        int totalFields = 0;
        int filledFields = 0;

        for (Map<String, Object> section : sections) {
            for (Map.Entry<String, Object> entry : section.entrySet()) {
                totalFields++;
                if (entry.getValue() != null
                        && !(entry.getValue() instanceof List && ((List<?>) entry.getValue()).isEmpty())
                        && !(entry.getValue() instanceof String && ((String) entry.getValue()).isEmpty())) {
                    filledFields++;
                }
            }
        }

        return totalFields > 0 ? (double) filledFields / totalFields : 0.0;
    }

    /**
     * Generate evidence anchors for all fields
     */
    private List<PaperSummaryDto.EvidenceAnchor> generateEvidenceAnchors(ExtractionContext context) {
        List<PaperSummaryDto.EvidenceAnchor> anchors = new ArrayList<>();

        // Add anchors for different sections
        for (ExtractionContext.SectionContent section : context.getSections()) {
            if (section.getType() != null) {
                anchors.add(PaperSummaryDto.EvidenceAnchor.builder()
                        .field(section.getType())
                        .page(section.getPageStart())
                        .source("section")
                        .sourceId(section.getId())
                        .confidence(0.9)
                        .build());
            }
        }

        // Add anchors for figures and tables
        for (ExtractionContext.FigureContent figure : context.getFigures()) {
            anchors.add(PaperSummaryDto.EvidenceAnchor.builder()
                    .field("figure")
                    .page(figure.getPage())
                    .source("figure")
                    .sourceId(figure.getId())
                    .confidence(0.95)
                    .build());
        }

        for (ExtractionContext.TableContent table : context.getTables()) {
            anchors.add(PaperSummaryDto.EvidenceAnchor.builder()
                    .field("table")
                    .page(table.getPage())
                    .source("table")
                    .sourceId(table.getId())
                    .confidence(0.95)
                    .build());
        }

        return anchors;
    }

    /**
     * Enrich summary with additional metrics
     */
    private void enrichSummaryWithMetrics(PaperSummaryDto summaryDTO, ExtractionContext context) {
        // Add any additional processing or validation here
        log.debug("Enriching summary with metrics");
    }

    /**
     * Save summary to database
     */
    private PaperSummary saveSummary(PaperSummaryDto dto, Paper paper, long startTime) {
        try {
            PaperSummary summary = PaperSummary.builder()
                    .paper(paper)
                    .oneLiner(dto.getOneLiner())
                    .keyContributions(objectMapper.writeValueAsString(dto.getKeyContributions()))
                    .methodOverview(dto.getMethodOverview())
                    .mainFindings(objectMapper.writeValueAsString(dto.getMainFindings()))
                    .limitations(objectMapper.writeValueAsString(dto.getLimitations()))
                    .applicability(objectMapper.writeValueAsString(dto.getApplicability()))
                    .studyType(PaperSummary.StudyType.valueOf(dto.getStudyType().toUpperCase()))
                    .researchQuestions(objectMapper.writeValueAsString(dto.getResearchQuestions()))
                    .datasets(objectMapper.writeValueAsString(dto.getDatasets()))
                    .participants(objectMapper.writeValueAsString(dto.getParticipants()))
                    .procedureOrPipeline(dto.getProcedureOrPipeline())
                    .baselinesOrControls(objectMapper.writeValueAsString(dto.getBaselinesOrControls()))
                    .metrics(objectMapper.writeValueAsString(dto.getMetrics()))
                    .statisticalAnalysis(objectMapper.writeValueAsString(dto.getStatisticalAnalysis()))
                    .computeResources(objectMapper.writeValueAsString(dto.getComputeResources()))
                    .implementationDetails(objectMapper.writeValueAsString(dto.getImplementationDetails()))
                    .artifacts(objectMapper.writeValueAsString(dto.getArtifacts()))
                    .reproducibilityNotes(dto.getReproducibilityNotes())
                    .reproScore(dto.getReproScore())
                    .ethics(objectMapper.writeValueAsString(dto.getEthics()))
                    .biasAndFairness(objectMapper.writeValueAsString(dto.getBiasAndFairness()))
                    .risksAndMisuse(objectMapper.writeValueAsString(dto.getRisksAndMisuse()))
                    .dataRights(dto.getDataRights())
                    .noveltyType(PaperSummary.NoveltyType.valueOf(
                            dto.getNoveltyType().toUpperCase()))
                    .positioning(objectMapper.writeValueAsString(dto.getPositioning()))
                    .relatedWorksKey(objectMapper.writeValueAsString(dto.getRelatedWorksKey()))
                    .impactNotes(dto.getImpactNotes())
                    .confidence(dto.getConfidence())
                    .evidenceAnchors(objectMapper.writeValueAsString(dto.getEvidenceAnchors()))
                    .threatsToValidity(objectMapper.writeValueAsString(dto.getThreatsToValidity()))
                    .domainClassification(objectMapper.writeValueAsString(dto.getDomainClassification()))
                    .technicalDepth(dto.getTechnicalDepth())
                    .interdisciplinaryConnections(
                            objectMapper.writeValueAsString(dto.getInterdisciplinaryConnections()))
                    .futureWork(objectMapper.writeValueAsString(dto.getFutureWork()))
                    .modelVersion("gemini-pro-1.5")
                    .responseSource(
                            isFallbackResponse
                                    ? PaperSummary.ResponseSource.FALLBACK
                                    : PaperSummary.ResponseSource.GEMINI_API)
                    .fallbackReason(fallbackReason)
                    .generationTimestamp(Instant.now())
                    .generationTimeSeconds((System.currentTimeMillis() - startTime) / 1000.0)
                    .validationStatus(PaperSummary.ValidationStatus.PENDING)
                    .build();

            return summaryRepository.save(summary);

        } catch (Exception e) {
            log.error("Failed to save summary", e);
            throw new RuntimeException("Failed to save summary", e);
        }
    }

    // Helper conversion methods
    @SuppressWarnings("unchecked")
    private List<String> convertToList(Object obj) {
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            return list.stream().map(item -> convertToString(item)).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private Double convertToDouble(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        return 0.0;
    }

    private String convertToString(Object obj) {
        if (obj == null) {
            return null;
        }
        return obj.toString();
    }

    private Boolean convertToBoolean(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        if (obj instanceof String) {
            String str = (String) obj;
            return "true".equalsIgnoreCase(str) || "1".equals(str) || "yes".equalsIgnoreCase(str);
        }
        if (obj instanceof Number) {
            return ((Number) obj).intValue() != 0;
        }
        return false;
    }

    private Integer convertToInteger(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<PaperSummaryDto.Finding> convertToFindings(Object obj) {
        if (obj instanceof List) {
            return ((List<Map<String, Object>>) obj)
                    .stream()
                    .map(map -> PaperSummaryDto.Finding.builder()
                            .task(convertToString(map.get("task")))
                            .metric(convertToString(map.get("metric")))
                            .value(convertToString(map.get("value")))
                            .comparator(convertToString(map.get("comparator")))
                            .delta(convertToString(map.get("delta")))
                            .significance(convertToString(map.get("significance")))
                            .build())
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    // Additional conversion methods for other complex types...
    @SuppressWarnings("unchecked")
    private List<PaperSummaryDto.DatasetInfo> convertToDatasets(Object obj) {
        if (obj instanceof List) {
            return ((List<Map<String, Object>>) obj)
                    .stream()
                    .map(map -> PaperSummaryDto.DatasetInfo.builder()
                            .name(convertToString(map.get("name")))
                            .domain(convertToString(map.get("domain")))
                            .size(convertToString(map.get("size")))
                            .splitInfo(convertToString(map.get("split_info")))
                            .license(convertToString(map.get("license")))
                            .url(convertToString(map.get("url")))
                            .description(convertToString(map.get("description")))
                            .build())
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private PaperSummaryDto.ParticipantInfo convertToParticipants(Object obj) {
        if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            return PaperSummaryDto.ParticipantInfo.builder()
                    .n((Integer) map.get("n"))
                    .demographics((String) map.get("demographics"))
                    .irbApproved((Boolean) map.get("irb_approved"))
                    .recruitmentMethod((String) map.get("recruitment_method"))
                    .compensationDetails((String) map.get("compensation_details"))
                    .build();
        }
        return null;
    }

    // Additional conversion methods for other complex types
    @SuppressWarnings("unchecked")
    private List<PaperSummaryDto.MetricInfo> convertToMetrics(Object obj) {
        if (obj instanceof List) {
            return ((List<Map<String, Object>>) obj)
                    .stream()
                    .map(map -> PaperSummaryDto.MetricInfo.builder()
                            .name(convertToString(map.get("name")))
                            .definition(convertToString(map.get("definition")))
                            .formula(convertToString(map.get("formula")))
                            .interpretation(convertToString(map.get("interpretation")))
                            .build())
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private PaperSummaryDto.ComputeInfo convertToCompute(Object obj) {
        if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            return PaperSummaryDto.ComputeInfo.builder()
                    .hardware(convertToString(map.get("hardware")))
                    .trainingTime(convertToString(map.get("training_time")))
                    .energyEstimateKwh(convertToDouble(map.get("energy_estimate_kwh")))
                    .cloudProvider(convertToString(map.get("cloud_provider")))
                    .estimatedCost(convertToDouble(map.get("estimated_cost")))
                    .gpuCount(convertToInteger(map.get("gpu_count")))
                    .build();
        }
        return null;
    }

    private PaperSummaryDto.ImplementationInfo convertToImplementation(Object obj) {
        if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            return PaperSummaryDto.ImplementationInfo.builder()
                    .frameworks(convertToList(map.get("frameworks")))
                    .keyHyperparams((Map<String, Object>) map.get("key_hyperparams"))
                    .language(convertToString(map.get("language")))
                    .dependencies(convertToString(map.get("dependencies")))
                    .codeLines(convertToInteger(map.get("code_lines")))
                    .build();
        }
        return null;
    }

    private PaperSummaryDto.ArtifactInfo convertToArtifacts(Object obj) {
        if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            return PaperSummaryDto.ArtifactInfo.builder()
                    .codeUrl(convertToString(map.get("code_url")))
                    .dataUrl(convertToString(map.get("data_url")))
                    .modelUrl(convertToString(map.get("model_url")))
                    .dockerImage(convertToString(map.get("docker_image")))
                    .configFiles(convertToString(map.get("config_files")))
                    .demoUrl(convertToString(map.get("demo_url")))
                    .supplementaryMaterial(convertToString(map.get("supplementary_material")))
                    .build();
        }
        return null;
    }

    private PaperSummaryDto.EthicsInfo convertToEthics(Object obj) {
        if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            return PaperSummaryDto.EthicsInfo.builder()
                    .irb(convertToBoolean(map.get("irb")))
                    .consent(convertToBoolean(map.get("consent")))
                    .sensitiveData(convertToBoolean(map.get("sensitive_data")))
                    .privacyMeasures(convertToString(map.get("privacy_measures")))
                    .dataAnonymization(convertToString(map.get("data_anonymization")))
                    .build();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<PaperSummaryDto.RelatedWork> convertToRelatedWorks(Object obj) {
        if (obj instanceof List) {
            return ((List<Map<String, Object>>) obj)
                    .stream()
                    .map(map -> PaperSummaryDto.RelatedWork.builder()
                            .citation((String) map.get("citation"))
                            .relation((String) map.get("relation"))
                            .description((String) map.get("description"))
                            .year(convertToString(map.get("year")))
                            .build())
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
