package org.solace.scholar_ai.project_service.dto.latex;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCheckpointRequest {
    private String checkpointName;
    private String contentBefore;
    private String contentAfter;
    private Long messageId;
    private Boolean setCurrent;
}
