package org.solace.scholar_ai.project_service.dto.citation;

import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitationSummaryDto {

    private Integer total;

    private Map<String, Integer> byType;

    private String contentHash;

    private Instant startedAt;

    private Instant finishedAt;
}
