package org.solace.scholar_ai.project_service.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Abstract highlight analysis results")
public class AbstractHighlightDto {

    @Schema(description = "List of highlights to apply to the abstract text")
    private List<Highlight> highlights;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Individual highlight information")
    public static class Highlight {
        @Schema(description = "The exact text to highlight", example = "transfer learning")
        private String text;

        @Schema(
                description = "Type of highlight",
                example = "keyword",
                allowableValues = {"keyword", "number", "concept", "method", "metric"})
        private String type;

        @Schema(description = "Starting character position in the abstract", example = "0")
        private int startIndex;

        @Schema(description = "Ending character position in the abstract", example = "15")
        private int endIndex;
    }
}
