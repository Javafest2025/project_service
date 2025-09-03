package org.solace.scholar_ai.project_service.exception;

/**
 * Exception thrown when attempting to generate a summary for a paper that
 * hasn't been extracted yet
 */
public class PaperNotExtractedException extends RuntimeException {

    public PaperNotExtractedException(String message) {
        super(message);
    }

    public PaperNotExtractedException(String message, Throwable cause) {
        super(message, cause);
    }
}
