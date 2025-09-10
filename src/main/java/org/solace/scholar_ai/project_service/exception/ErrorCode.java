package org.solace.scholar_ai.project_service.exception;

import lombok.Getter;

/**
 * Enumeration of error codes used across the application.
 * Each error code includes a user-friendly suggestion for resolution.
 */
@Getter
public enum ErrorCode {
    // Client Validation Errors
    INVALID_REQUEST("Please review the validation errors and correct your request."),
    INVALID_PARAMETER_TYPE("Please ensure the parameter value matches the required type."),
    CONSTRAINT_VIOLATION("Please check the input constraints in the API documentation."),
    MISSING_PARAMETER("Please include all required parameters as specified in the documentation."),
    MALFORMED_JSON("Please verify the JSON syntax and data types in your request."),
    INVALID_ARGUMENT("Please check the argument values against the API specifications."),
    UNSUPPORTED_MEDIA_TYPE("Please use one of the supported media types for this endpoint."),
    // Authentication & Authorization Errors
    ACCESS_DENIED("Please ensure you have the necessary permissions or authenticate properly."),
    // Resource & Method Errors
    RESOURCE_NOT_FOUND("Please verify the requested resource exists and the URL is correct."),
    METHOD_NOT_ALLOWED("Please use one of the supported HTTP methods for this endpoint."),
    // System Errors
    INTERNAL_ERROR("Please try again later or contact support if the issue persists."),
    EMAIL_SENDING_FAILED("Please check the email service configuration and try again."),
    EXTERNAL_API_ERROR("External API request failed. Please try again later or contact support."),
    EXTERNAL_SERVICE_ERROR("External service communication failed. Please try again later."),
    DUPLICATE("Please ensure the resource you're trying to create does not already exist."),
    VALIDATION_ERROR("Please review the validation errors and correct your request."),
    CONFIGURATION_ERROR("Please check the application configuration for any issues."),
    // Author-specific errors
    AUTHOR_NOT_FOUND("The requested author could not be found. Please check the author name or ID."),
    DATA_CONVERSION_ERROR("Error processing data. Please verify the data format and try again."),
    // Paper-specific errors
    PAPER_NOT_FOUND("The requested paper could not be found. Please check the paper ID."),
    PAPER_NOT_EXTRACTED("The paper has not been extracted yet. Please extract the paper first."),
    // Gap analysis specific errors
    GAP_ANALYSIS_NOT_FOUND("The requested gap analysis could not be found. Please check the gap analysis ID."),
    GAP_ANALYSIS_REQUEST_FAILED("Failed to initiate gap analysis. Please try again later."),
    INVALID_OPERATION("The requested operation is not valid for the current state of the resource.");

    private final String suggestion;

    ErrorCode(String suggestion) {
        this.suggestion = suggestion;
    }
}
