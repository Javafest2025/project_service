package org.solace.scholar_ai.project_service.dto.latex;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompileLatexRequestDTO {

    @NotBlank(message = "LaTeX content is required")
    private String latexContent;
}
