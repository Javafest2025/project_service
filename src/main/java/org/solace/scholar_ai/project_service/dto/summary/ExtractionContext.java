package org.solace.scholar_ai.project_service.dto.summary;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Context object containing all extracted data for summary generation
 */
@Data
@Builder
public class ExtractionContext {
    private String title;
    private String abstractText;
    private List<SectionContent> sections;
    private List<FigureContent> figures;
    private List<TableContent> tables;
    private List<EquationContent> equations;
    private List<CodeBlockContent> codeBlocks;
    private List<ReferenceContent> references;
    private List<EntityContent> entities;
    private Integer pageCount;
    private String language;

    @Data
    @Builder
    public static class SectionContent {
        private String id;
        private String title;
        private String type;
        private Integer level;
        private List<String> paragraphs;
        private Integer pageStart;
        private Integer pageEnd;
    }

    @Data
    @Builder
    public static class FigureContent {
        private String id;
        private String label;
        private String caption;
        private Integer page;
        private String ocrText;
    }

    @Data
    @Builder
    public static class TableContent {
        private String id;
        private String label;
        private String caption;
        private Integer page;
        private String headers;
        private String rows;
        private String html;
    }

    @Data
    @Builder
    public static class EquationContent {
        private String id;
        private String label;
        private String latexContent;
        private Integer page;
    }

    @Data
    @Builder
    public static class CodeBlockContent {
        private String id;
        private String language;
        private String code;
        private Integer page;
    }

    @Data
    @Builder
    public static class ReferenceContent {
        private String id;
        private String title;
        private String authors;
        private Integer year;
        private String venue;
        private String doi;
        private String url;
    }

    @Data
    @Builder
    public static class EntityContent {
        private String id;
        private String text;
        private String type;
        private String metadata;
    }
}
