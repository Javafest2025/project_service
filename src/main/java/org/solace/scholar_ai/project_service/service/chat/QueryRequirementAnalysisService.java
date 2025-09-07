package org.solace.scholar_ai.project_service.service.chat;

import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class QueryRequirementAnalysisService {

    public enum DataRequirement {
        AUTHORS,
        ABSTRACT,
        INTRODUCTION,
        METHODOLOGY,
        RESULTS,
        CONCLUSION,
        REFERENCES,
        FIGURES,
        TABLES,
        EQUATIONS,
        FULL_PAPER_CONTENT,
        ALL_SECTIONS,
        TITLE,
        ALGORITHMS,
        EXPERIMENTAL_SETUP
    }

    public Set<DataRequirement> analyzeQueryRequirements(String query) {
        Set<DataRequirement> requirements = new HashSet<>();

        String lowerQuery = query.toLowerCase();

        if (lowerQuery.contains("author") || lowerQuery.contains("researcher")) {
            requirements.add(DataRequirement.AUTHORS);
        }
        if (lowerQuery.contains("title") || lowerQuery.contains("name")) {
            requirements.add(DataRequirement.TITLE);
        }
        if (lowerQuery.contains("abstract") || lowerQuery.contains("summary")) {
            requirements.add(DataRequirement.ABSTRACT);
        }
        if (lowerQuery.contains("method") || lowerQuery.contains("approach")) {
            requirements.add(DataRequirement.METHODOLOGY);
        }
        if (lowerQuery.contains("result") || lowerQuery.contains("finding")) {
            requirements.add(DataRequirement.RESULTS);
        }
        if (lowerQuery.contains("algorithm")) {
            requirements.add(DataRequirement.ALGORITHMS);
        }
        if (lowerQuery.contains("experiment") || lowerQuery.contains("setup")) {
            requirements.add(DataRequirement.EXPERIMENTAL_SETUP);
        }

        // Default to including some basic information if no specific requirements detected
        if (requirements.isEmpty()) {
            requirements.add(DataRequirement.TITLE);
            requirements.add(DataRequirement.ABSTRACT);
        }

        return requirements;
    }

    public boolean shouldIncludeAuthors(Set<DataRequirement> requirements) {
        return requirements.contains(DataRequirement.AUTHORS);
    }
}
