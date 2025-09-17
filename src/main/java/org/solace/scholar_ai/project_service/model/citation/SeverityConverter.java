package org.solace.scholar_ai.project_service.model.citation;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class SeverityConverter implements AttributeConverter<CitationIssue.Severity, String> {

    @Override
    public String convertToDatabaseColumn(CitationIssue.Severity severity) {
        if (severity == null) {
            return null;
        }
        return severity.getValue();
    }

    @Override
    public CitationIssue.Severity convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return CitationIssue.Severity.fromValue(value);
    }
}
