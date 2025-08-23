package org.solace.scholar_ai.project_service.constant.todo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.stream.Stream;

public enum TodoPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT;

    @JsonValue
    public String toValue() {
        return this.name().toLowerCase();
    }

    @JsonCreator
    public static TodoPriority fromValue(String value) {
        if (value == null) {
            return null;
        }
        return Stream.of(TodoPriority.values())
                .filter(priority -> priority.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown enum type " + value + ", Allowed values are "
                        + Stream.of(values()).map(TodoPriority::name).toString()));
    }
}
