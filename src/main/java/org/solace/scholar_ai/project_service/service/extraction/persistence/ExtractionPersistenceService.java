package org.solace.scholar_ai.project_service.service.extraction.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.event.extraction.ExtractionCompletedEvent;
import org.solace.scholar_ai.project_service.model.extraction.*;
import org.solace.scholar_ai.project_service.model.paper.Paper;
import org.solace.scholar_ai.project_service.repository.extraction.PaperExtractionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for persisting extraction results to the database
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractionPersistenceService {

    private final PaperExtractionRepository paperExtractionRepository;
    private final ObjectMapper objectMapper;

    /**
     * Persist extraction results to database
     *
     * @param paper The paper entity
     * @param event The extraction completion event
     */
    @Transactional
    public void persistExtractionResult(Paper paper, ExtractionCompletedEvent event) {
        try {
            log.info("Persisting extraction results for paper: {}", paper.getId());

            // Parse the extraction result JSON
            JsonNode resultJson = objectMapper.readTree(event.extractionResult());

            // Create or update PaperExtraction entity
            PaperExtraction paperExtraction = createPaperExtraction(paper, event, resultJson);

            // Save the main extraction entity
            paperExtraction = paperExtractionRepository.save(paperExtraction);

            // Process and save all extracted content
            processExtractedSections(paperExtraction, resultJson.get("sections"));
            processExtractedFigures(paperExtraction, resultJson.get("figures"));
            processExtractedTables(paperExtraction, resultJson.get("tables"));
            processExtractedEquations(paperExtraction, resultJson.get("equations"));
            processExtractedCodeBlocks(paperExtraction, resultJson.get("code_blocks"));
            processExtractedReferences(paperExtraction, resultJson.get("references"));
            processExtractedEntities(paperExtraction, resultJson.get("entities"));

            // Save the final entity with all relationships
            paperExtractionRepository.save(paperExtraction);

            log.info("Successfully persisted extraction results for paper: {}", paper.getId());

        } catch (Exception e) {
            log.error("Failed to persist extraction results for paper {}: {}", paper.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to persist extraction results", e);
        }
    }

    private PaperExtraction createPaperExtraction(Paper paper, ExtractionCompletedEvent event, JsonNode resultJson) {
        PaperExtraction extraction = PaperExtraction.builder()
                .paper(paper)
                .extractionId(resultJson.get("id").asText())
                .pdfHash(resultJson.get("pdf_hash").asText())
                .extractionTimestamp(
                        parseTimestamp(resultJson.get("extraction_timestamp").asText()))
                .processingTime(event.processingTime())
                .extractionCoverage(event.extractionCoverage())
                .build();

        // Extract metadata
        JsonNode metadata = resultJson.get("metadata");
        if (metadata != null) {
            extraction.setTitle(metadata.get("title").asText());
            extraction.setAbstractText(getTextValue(metadata, "abstract"));
            extraction.setLanguage(getTextValue(metadata, "language"));
            extraction.setPageCount(getIntValue(metadata, "page_count"));
        }

        // Set processing metadata
        extraction.setExtractionMethods(getTextValue(resultJson, "extraction_methods"));
        extraction.setErrors(event.errors());
        extraction.setWarnings(event.warnings());
        extraction.setConfidenceScores(event.confidenceScores());

        return extraction;
    }

    private void processExtractedSections(PaperExtraction paperExtraction, JsonNode sectionsNode) {
        if (sectionsNode == null || !sectionsNode.isArray())
            return;

        int orderIndex = 0;
        for (JsonNode sectionNode : sectionsNode) {
            ExtractedSection section = createExtractedSection(paperExtraction, sectionNode, orderIndex++);
            paperExtraction.addSection(section);

            // Process paragraphs
            JsonNode paragraphsNode = sectionNode.get("paragraphs");
            if (paragraphsNode != null && paragraphsNode.isArray()) {
                int paragraphIndex = 0;
                for (JsonNode paragraphNode : paragraphsNode) {
                    ExtractedParagraph paragraph = createExtractedParagraph(section, paragraphNode, paragraphIndex++);
                    section.addParagraph(paragraph);
                }
            }
        }
    }

    private ExtractedSection createExtractedSection(
            PaperExtraction paperExtraction, JsonNode sectionNode, int orderIndex) {
        return ExtractedSection.builder()
                .paperExtraction(paperExtraction)
                .sectionId(getTextValue(sectionNode, "id"))
                .label(getTextValue(sectionNode, "label"))
                .title(getTextValue(sectionNode, "title"))
                .sectionType(getTextValue(sectionNode, "type"))
                .level(getIntValue(sectionNode, "level"))
                .pageStart(getIntValue(sectionNode, "page_start"))
                .pageEnd(getIntValue(sectionNode, "page_end"))
                .orderIndex(orderIndex)
                .build();
    }

    private ExtractedParagraph createExtractedParagraph(
            ExtractedSection section, JsonNode paragraphNode, int orderIndex) {
        ExtractedParagraph paragraph = ExtractedParagraph.builder()
                .section(section)
                .text(getTextValue(paragraphNode, "text"))
                .page(getIntValue(paragraphNode, "page"))
                .orderIndex(orderIndex)
                .build();

        // Extract bounding box if available
        JsonNode bboxNode = paragraphNode.get("bbox");
        if (bboxNode != null) {
            paragraph.setBboxX1(getDoubleValue(bboxNode, "x1"));
            paragraph.setBboxY1(getDoubleValue(bboxNode, "y1"));
            paragraph.setBboxX2(getDoubleValue(bboxNode, "x2"));
            paragraph.setBboxY2(getDoubleValue(bboxNode, "y2"));
        }

        // Extract style if available
        JsonNode styleNode = paragraphNode.get("style");
        if (styleNode != null) {
            paragraph.setStyle(styleNode.toString());
        }

        return paragraph;
    }

    private void processExtractedFigures(PaperExtraction paperExtraction, JsonNode figuresNode) {
        if (figuresNode == null || !figuresNode.isArray())
            return;

        int orderIndex = 0;
        for (JsonNode figureNode : figuresNode) {
            ExtractedFigure figure = createExtractedFigure(paperExtraction, figureNode, orderIndex++);
            paperExtraction.addFigure(figure);
        }
    }

    private ExtractedFigure createExtractedFigure(
            PaperExtraction paperExtraction, JsonNode figureNode, int orderIndex) {
        ExtractedFigure figure = ExtractedFigure.builder()
                .paperExtraction(paperExtraction)
                .figureId(getTextValue(figureNode, "id"))
                .label(getTextValue(figureNode, "label"))
                .caption(getTextValue(figureNode, "caption"))
                .page(getIntValue(figureNode, "page"))
                .figureType(getTextValue(figureNode, "type"))
                .imagePath(getTextValue(figureNode, "image_path"))
                .thumbnailPath(getTextValue(figureNode, "thumbnail_path"))
                .ocrText(getTextValue(figureNode, "ocr_text"))
                .ocrConfidence(getDoubleValue(figureNode, "ocr_confidence"))
                .orderIndex(orderIndex)
                .build();

        // Extract bounding box
        JsonNode bboxNode = figureNode.get("bbox");
        if (bboxNode != null) {
            figure.setBboxX1(getDoubleValue(bboxNode, "x1"));
            figure.setBboxY1(getDoubleValue(bboxNode, "y1"));
            figure.setBboxX2(getDoubleValue(bboxNode, "x2"));
            figure.setBboxY2(getDoubleValue(bboxNode, "y2"));
            figure.setBboxConfidence(getDoubleValue(bboxNode, "confidence"));
        }

        // Extract references
        JsonNode referencesNode = figureNode.get("references");
        if (referencesNode != null) {
            figure.setReferences(referencesNode.toString());
        }

        return figure;
    }

    private void processExtractedTables(PaperExtraction paperExtraction, JsonNode tablesNode) {
        if (tablesNode == null || !tablesNode.isArray())
            return;

        int orderIndex = 0;
        for (JsonNode tableNode : tablesNode) {
            ExtractedTable table = createExtractedTable(paperExtraction, tableNode, orderIndex++);
            paperExtraction.addTable(table);
        }
    }

    private ExtractedTable createExtractedTable(PaperExtraction paperExtraction, JsonNode tableNode, int orderIndex) {
        ExtractedTable table = ExtractedTable.builder()
                .paperExtraction(paperExtraction)
                .tableId(getTextValue(tableNode, "id"))
                .label(getTextValue(tableNode, "label"))
                .caption(getTextValue(tableNode, "caption"))
                .page(getIntValue(tableNode, "page"))
                .csvPath(getTextValue(tableNode, "csv_path"))
                .html(getTextValue(tableNode, "html"))
                .orderIndex(orderIndex)
                .build();

        // Extract bounding box
        JsonNode bboxNode = tableNode.get("bbox");
        if (bboxNode != null) {
            table.setBboxX1(getDoubleValue(bboxNode, "x1"));
            table.setBboxY1(getDoubleValue(bboxNode, "y1"));
            table.setBboxX2(getDoubleValue(bboxNode, "x2"));
            table.setBboxY2(getDoubleValue(bboxNode, "y2"));
            table.setBboxConfidence(getDoubleValue(bboxNode, "confidence"));
        }

        // Extract table structure
        JsonNode headersNode = tableNode.get("headers");
        if (headersNode != null) {
            table.setHeaders(headersNode.toString());
        }

        JsonNode rowsNode = tableNode.get("rows");
        if (rowsNode != null) {
            table.setRows(rowsNode.toString());
        }

        JsonNode structureNode = tableNode.get("structure");
        if (structureNode != null) {
            table.setStructure(structureNode.toString());
        }

        JsonNode referencesNode = tableNode.get("references");
        if (referencesNode != null) {
            table.setReferences(referencesNode.toString());
        }

        return table;
    }

    private void processExtractedEquations(PaperExtraction paperExtraction, JsonNode equationsNode) {
        if (equationsNode == null || !equationsNode.isArray())
            return;

        int orderIndex = 0;
        for (JsonNode equationNode : equationsNode) {
            ExtractedEquation equation = createExtractedEquation(paperExtraction, equationNode, orderIndex++);
            paperExtraction.addEquation(equation);
        }
    }

    private ExtractedEquation createExtractedEquation(
            PaperExtraction paperExtraction, JsonNode equationNode, int orderIndex) {
        ExtractedEquation equation = ExtractedEquation.builder()
                .paperExtraction(paperExtraction)
                .equationId(getTextValue(equationNode, "id"))
                .label(getTextValue(equationNode, "label"))
                .latex(getTextValue(equationNode, "latex"))
                .mathml(getTextValue(equationNode, "mathml"))
                .page(getIntValue(equationNode, "page"))
                .isInline(getBooleanValue(equationNode, "inline"))
                .orderIndex(orderIndex)
                .build();

        // Extract bounding box
        JsonNode bboxNode = equationNode.get("bbox");
        if (bboxNode != null) {
            equation.setBboxX1(getDoubleValue(bboxNode, "x1"));
            equation.setBboxY1(getDoubleValue(bboxNode, "y1"));
            equation.setBboxX2(getDoubleValue(bboxNode, "x2"));
            equation.setBboxY2(getDoubleValue(bboxNode, "y2"));
        }

        return equation;
    }

    private void processExtractedCodeBlocks(PaperExtraction paperExtraction, JsonNode codeBlocksNode) {
        if (codeBlocksNode == null || !codeBlocksNode.isArray())
            return;

        int orderIndex = 0;
        for (JsonNode codeBlockNode : codeBlocksNode) {
            ExtractedCodeBlock codeBlock = createExtractedCodeBlock(paperExtraction, codeBlockNode, orderIndex++);
            paperExtraction.addCodeBlock(codeBlock);
        }
    }

    private ExtractedCodeBlock createExtractedCodeBlock(
            PaperExtraction paperExtraction, JsonNode codeBlockNode, int orderIndex) {
        ExtractedCodeBlock codeBlock = ExtractedCodeBlock.builder()
                .paperExtraction(paperExtraction)
                .codeId(getTextValue(codeBlockNode, "id"))
                .language(getTextValue(codeBlockNode, "language"))
                .code(getTextValue(codeBlockNode, "code"))
                .page(getIntValue(codeBlockNode, "page"))
                .context(getTextValue(codeBlockNode, "context"))
                .hasLineNumbers(getBooleanValue(codeBlockNode, "line_numbers"))
                .orderIndex(orderIndex)
                .build();

        // Extract bounding box
        JsonNode bboxNode = codeBlockNode.get("bbox");
        if (bboxNode != null) {
            codeBlock.setBboxX1(getDoubleValue(bboxNode, "x1"));
            codeBlock.setBboxY1(getDoubleValue(bboxNode, "y1"));
            codeBlock.setBboxX2(getDoubleValue(bboxNode, "x2"));
            codeBlock.setBboxY2(getDoubleValue(bboxNode, "y2"));
        }

        return codeBlock;
    }

    private void processExtractedReferences(PaperExtraction paperExtraction, JsonNode referencesNode) {
        if (referencesNode == null || !referencesNode.isArray())
            return;

        int orderIndex = 0;
        for (JsonNode referenceNode : referencesNode) {
            ExtractedReference reference = createExtractedReference(paperExtraction, referenceNode, orderIndex++);
            paperExtraction.addReference(reference);
        }
    }

    private ExtractedReference createExtractedReference(
            PaperExtraction paperExtraction, JsonNode referenceNode, int orderIndex) {
        ExtractedReference reference = ExtractedReference.builder()
                .paperExtraction(paperExtraction)
                .referenceId(getTextValue(referenceNode, "id"))
                .rawText(getTextValue(referenceNode, "raw_text"))
                .title(getTextValue(referenceNode, "title"))
                .year(getIntValue(referenceNode, "year"))
                .venue(getTextValue(referenceNode, "venue"))
                .doi(getTextValue(referenceNode, "doi"))
                .url(getTextValue(referenceNode, "url"))
                .arxivId(getTextValue(referenceNode, "arxiv_id"))
                .citationCount(getIntValue(referenceNode, "citation_count"))
                .orderIndex(orderIndex)
                .build();

        // Extract authors
        JsonNode authorsNode = referenceNode.get("authors");
        if (authorsNode != null) {
            reference.setAuthors(authorsNode.toString());
        }

        // Extract enrichment data
        JsonNode crossrefNode = referenceNode.get("crossref_data");
        if (crossrefNode != null) {
            reference.setCrossrefData(crossrefNode.toString());
        }

        JsonNode openalexNode = referenceNode.get("openalex_data");
        if (openalexNode != null) {
            reference.setOpenalexData(openalexNode.toString());
        }

        JsonNode unpaywallNode = referenceNode.get("unpaywall_data");
        if (unpaywallNode != null) {
            reference.setUnpaywallData(unpaywallNode.toString());
        }

        JsonNode citedBySectionsNode = referenceNode.get("cited_by_sections");
        if (citedBySectionsNode != null) {
            reference.setCitedBySections(citedBySectionsNode.toString());
        }

        return reference;
    }

    private void processExtractedEntities(PaperExtraction paperExtraction, JsonNode entitiesNode) {
        if (entitiesNode == null || !entitiesNode.isArray())
            return;

        int orderIndex = 0;
        for (JsonNode entityNode : entitiesNode) {
            ExtractedEntity entity = createExtractedEntity(paperExtraction, entityNode, orderIndex++);
            paperExtraction.addEntity(entity);
        }
    }

    private ExtractedEntity createExtractedEntity(
            PaperExtraction paperExtraction, JsonNode entityNode, int orderIndex) {
        return ExtractedEntity.builder()
                .paperExtraction(paperExtraction)
                .entityId(getTextValue(entityNode, "id"))
                .entityType(getTextValue(entityNode, "type"))
                .name(getTextValue(entityNode, "name"))
                .uri(getTextValue(entityNode, "uri"))
                .page(getIntValue(entityNode, "page"))
                .context(getTextValue(entityNode, "context"))
                .confidence(getDoubleValue(entityNode, "confidence"))
                .orderIndex(orderIndex)
                .build();
    }

    // Helper methods for safe JSON extraction
    private String getTextValue(JsonNode node, String fieldName) {
        if (node != null && node.has(fieldName) && !node.get(fieldName).isNull()) {
            return node.get(fieldName).asText();
        }
        return null;
    }

    private Integer getIntValue(JsonNode node, String fieldName) {
        if (node != null && node.has(fieldName) && !node.get(fieldName).isNull()) {
            return node.get(fieldName).asInt();
        }
        return null;
    }

    private Double getDoubleValue(JsonNode node, String fieldName) {
        if (node != null && node.has(fieldName) && !node.get(fieldName).isNull()) {
            return node.get(fieldName).asDouble();
        }
        return null;
    }

    private Boolean getBooleanValue(JsonNode node, String fieldName) {
        if (node != null && node.has(fieldName) && !node.get(fieldName).isNull()) {
            return node.get(fieldName).asBoolean();
        }
        return false;
    }

    private Instant parseTimestamp(String timestamp) {
        try {
            return Instant.parse(timestamp.replace(" ", "T") + "Z");
        } catch (Exception e) {
            log.warn("Failed to parse timestamp: {}, using current time", timestamp);
            return Instant.now();
        }
    }
}
