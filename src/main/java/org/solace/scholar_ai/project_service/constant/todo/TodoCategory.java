package org.solace.scholar_ai.project_service.constant.todo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.stream.Stream;

public enum TodoCategory {
    // Research Lifecycle
    LITERATURE_REVIEW,
    EXPERIMENT,
    DATA_COLLECTION,
    DATA_ANALYSIS,
    MODELING,
    WRITING,
    REVIEW,
    SUBMISSION,
    PRESENTATION,

    // Project & Collaboration
    COLLABORATION,
    MEETING,
    DEADLINE,
    FUNDING,
    ADMINISTRATIVE,

    // Personal Productivity
    PERSONAL,
    MISC;

    @JsonValue
    public String toValue() {
        return this.name().toLowerCase();
    }

    @JsonCreator
    public static TodoCategory fromValue(String value) {
        if (value == null) {
            return null;
        }
        return Stream.of(TodoCategory.values())
                .filter(category -> category.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown enum type " + value + ", Allowed values are "
                        + Stream.of(values()).map(TodoCategory::name).toList()));
    }
}
