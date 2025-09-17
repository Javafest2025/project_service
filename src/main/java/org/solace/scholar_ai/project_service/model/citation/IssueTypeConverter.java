package org.solace.scholar_ai.project_service.model.citation;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class IssueTypeConverter implements AttributeConverter<CitationIssue.IssueType, String> {

    @Override
    public String convertToDatabaseColumn(CitationIssue.IssueType issueType) {
        if (issueType == null) {
            return null;
        }
        return issueType.getValue();
    }

    @Override
    public CitationIssue.IssueType convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return CitationIssue.IssueType.fromValue(value);
    }
}
