package org.solace.scholar_ai.project_service.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Abstract analysis insights")
public class AbstractAnalysisDto {

    @Schema(description = "Primary research area or domain", example = "Transfer Learning")
    private String focus;

    @Schema(description = "Methodology or approach used", example = "Comprehensive Review")
    private String approach;

    @Schema(description = "Key contribution or emphasis", example = "Trustworthiness")
    private String emphasis;

    @Schema(description = "Research methodology", example = "Literature Review")
    private String methodology;

    @Schema(description = "Potential impact or significance", example = "Improved reliability and trustworthiness")
    private String impact;

    @Schema(description = "Key challenges addressed", example = "Knowledge Transferability")
    private String challenges;
}
