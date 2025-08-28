package org.solace.scholar_ai.project_service.constant.todo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.stream.Stream;

public enum TodoStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    @JsonValue
    public String toValue() {
        return this.name().toLowerCase();
    }

    @JsonCreator
    public static TodoStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        return Stream.of(TodoStatus.values())
                .filter(status -> status.name().replace("_", "").equalsIgnoreCase(value.replace("_", "")))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown enum type " + value + ", Allowed values are "
                        + Stream.of(values()).map(TodoStatus::name).toString()));
    }
}
