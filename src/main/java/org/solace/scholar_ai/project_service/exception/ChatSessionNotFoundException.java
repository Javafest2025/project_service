package org.solace.scholar_ai.project_service.exception;

/**
 * Exception thrown when a chat session is not found
 */
public class ChatSessionNotFoundException extends RuntimeException {

    public ChatSessionNotFoundException(String message) {
        super(message);
    }

    public ChatSessionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
