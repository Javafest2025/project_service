package org.solace.scholar_ai.project_service.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Data transfer object for image upload response")
public record ImageUploadDto(
        @Schema(description = "Unique image identifier", example = "550e8400-e29b-41d4-a716-446655440000") UUID imageId,
        @Schema(description = "Original filename", example = "screenshot.png") String originalFilename,
        @Schema(
                        description = "Generated filename for storage",
                        example = "img_550e8400-e29b-41d4-a716-446655440000.png")
                String storedFilename,
        @Schema(description = "File size in bytes", example = "1024000") Long fileSize,
        @Schema(description = "MIME type", example = "image/png") String mimeType,
        @Schema(description = "Upload timestamp", example = "2024-01-15T10:30:00Z") Instant uploadedAt,
        @Schema(
                        description = "Public URL to access the image",
                        example = "/api/v1/projects/{projectId}/notes/images/{imageId}")
                String imageUrl) {}
