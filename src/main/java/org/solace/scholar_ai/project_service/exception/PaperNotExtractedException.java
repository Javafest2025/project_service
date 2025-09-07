package org.solace.scholar_ai.project_service.exception;

import java.util.UUID;
import lombok.Getter;

/**
 * Exception thrown when attempting to generate a summary for a paper that
 * hasn't been extracted yet
 */
@Getter
public class PaperNotExtractedException extends RuntimeException {
    private final UUID paperId;

    public PaperNotExtractedException(String message) {
        super(message);
        this.paperId = null;
    }

    public PaperNotExtractedException(UUID paperId) {
        super("Paper has not been extracted yet. Please wait for extraction to complete. Paper ID: " + paperId);
        this.paperId = paperId;
    }

    public PaperNotExtractedException(String message, UUID paperId) {
        super(message);
        this.paperId = paperId;
    }

    public PaperNotExtractedException(String message, Throwable cause) {
        super(message, cause);
        this.paperId = null;
    }
}
