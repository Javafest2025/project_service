package org.solace.scholar_ai.project_service.dto.chat;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.solace.scholar_ai.project_service.constant.CommandType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedCommand {
    private CommandType commandType;
    private Map<String, Object> parameters;
    private String originalQuery;
    private String naturalResponse;
}
