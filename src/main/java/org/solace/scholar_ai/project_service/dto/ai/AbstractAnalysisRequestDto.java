package org.solace.scholar_ai.project_service.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for abstract analysis")
public class AbstractAnalysisRequestDto {

    @NotBlank(message = "Abstract text is required")
    @Size(min = 10, max = 10000, message = "Abstract text must be between 10 and 10000 characters")
    @Schema(description = "The abstract text to analyze", example = "Transfer learning aims to transfer knowledge...")
    private String abstractText;
}
