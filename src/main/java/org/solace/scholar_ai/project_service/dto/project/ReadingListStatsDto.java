package org.solace.scholar_ai.project_service.dto.project;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingListStatsDto {
    private int totalPapers;
    private int readPapers;
    private int unreadPapers;
    private String timeRange;
}
