package org.solace.scholar_ai.project_service.exception;

import java.util.UUID;
import lombok.Getter;

/**
 * Exception thrown when a paper is not found
 */
@Getter
public class PaperNotFoundException extends RuntimeException {
    private final UUID paperId;

    public PaperNotFoundException(String message) {
        super(message);
        this.paperId = null;
    }

    public PaperNotFoundException(UUID paperId) {
        super("Paper not found with ID: " + paperId);
        this.paperId = paperId;
    }

    public PaperNotFoundException(String message, UUID paperId) {
        super(message);
        this.paperId = paperId;
    }

    public PaperNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.paperId = null;
    }
}
