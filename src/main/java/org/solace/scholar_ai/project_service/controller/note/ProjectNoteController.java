package org.solace.scholar_ai.project_service.controller.note;

import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.note.AIContentRequestDto;
import org.solace.scholar_ai.project_service.dto.note.AIContentResponseDto;
import org.solace.scholar_ai.project_service.dto.note.CreateNoteDto;
import org.solace.scholar_ai.project_service.dto.note.ImageUploadDto;
import org.solace.scholar_ai.project_service.dto.note.NoteDto;
import org.solace.scholar_ai.project_service.dto.note.PaperSuggestionDto;
import org.solace.scholar_ai.project_service.dto.note.UpdateNoteDto;
import org.solace.scholar_ai.project_service.dto.response.APIResponse;
import org.solace.scholar_ai.project_service.model.note.NoteImage;
import org.solace.scholar_ai.project_service.service.ai.AIContentService;
import org.solace.scholar_ai.project_service.service.note.NoteImageService;
import org.solace.scholar_ai.project_service.service.note.PaperMentionService;
import org.solace.scholar_ai.project_service.service.note.ProjectNoteService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("api/v1/projects/{projectId}/notes")
@RequiredArgsConstructor
public class ProjectNoteController {

    private final ProjectNoteService projectNoteService;
    private final NoteImageService noteImageService;
    private final PaperMentionService paperMentionService;
    private final AIContentService aiContentService;

    /**
     * Get all notes for a project
     */
    @GetMapping
    public ResponseEntity<APIResponse<List<NoteDto>>> getNotes(@PathVariable UUID projectId) {
        try {
            log.info("Get notes for project {} endpoint hit", projectId);

            List<NoteDto> notes = projectNoteService.getNotesByProjectId(projectId, null);

            return ResponseEntity.ok(APIResponse.success(HttpStatus.OK.value(), "Notes retrieved successfully", notes));
        } catch (RuntimeException e) {
            log.error("Error retrieving notes for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error retrieving notes for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve notes", null));
        }
    }

    /**
     * Get a specific note by ID
     */
    @GetMapping("/{noteId}")
    public ResponseEntity<APIResponse<NoteDto>> getNote(@PathVariable UUID projectId, @PathVariable UUID noteId) {
        try {
            log.info("Get note {} for project {} endpoint hit", noteId, projectId);

            NoteDto note = projectNoteService.getNoteById(projectId, noteId, null);

            return ResponseEntity.ok(APIResponse.success(HttpStatus.OK.value(), "Note retrieved successfully", note));
        } catch (RuntimeException e) {
            log.error("Error retrieving note {} for project {}: {}", noteId, projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error retrieving note {} for project {}: {}", noteId, projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve note", null));
        }
    }

    /**
     * Create a new note
     */
    @PostMapping
    public ResponseEntity<APIResponse<NoteDto>> createNote(
            @PathVariable UUID projectId, @Valid @RequestBody CreateNoteDto createNoteDto) {
        try {
            log.info("Create note for project {} endpoint hit", projectId);

            NoteDto createdNote = projectNoteService.createNote(projectId, createNoteDto, null);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(APIResponse.success(HttpStatus.CREATED.value(), "Note created successfully", createdNote));
        } catch (RuntimeException e) {
            log.error("Error creating note for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error creating note for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to create note", null));
        }
    }

    /**
     * Update an existing note
     */
    @PutMapping("/{noteId}")
    public ResponseEntity<APIResponse<NoteDto>> updateNote(
            @PathVariable UUID projectId, @PathVariable UUID noteId, @Valid @RequestBody UpdateNoteDto updateNoteDto) {
        try {
            log.info("Update note {} for project {} endpoint hit", noteId, projectId);

            NoteDto updatedNote = projectNoteService.updateNote(projectId, noteId, updateNoteDto, null);

            return ResponseEntity.ok(
                    APIResponse.success(HttpStatus.OK.value(), "Note updated successfully", updatedNote));
        } catch (RuntimeException e) {
            log.error("Error updating note {} for project {}: {}", noteId, projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error updating note {} for project {}: {}", noteId, projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to update note", null));
        }
    }

    /**
     * Delete a note
     */
    @DeleteMapping("/{noteId}")
    public ResponseEntity<APIResponse<String>> deleteNote(@PathVariable UUID projectId, @PathVariable UUID noteId) {
        try {
            log.info("Delete note {} for project {} endpoint hit", noteId, projectId);

            projectNoteService.deleteNote(projectId, noteId, null);

            return ResponseEntity.ok(APIResponse.success(HttpStatus.OK.value(), "Note deleted successfully", null));
        } catch (RuntimeException e) {
            log.error("Error deleting note {} for project {}: {}", noteId, projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error deleting note {} for project {}: {}", noteId, projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to delete note", null));
        }
    }

    /**
     * Toggle favorite status of a note
     */
    @PutMapping("/{noteId}/favorite")
    public ResponseEntity<APIResponse<NoteDto>> toggleNoteFavorite(
            @PathVariable UUID projectId, @PathVariable UUID noteId) {
        try {
            log.info("Toggle favorite for note {} in project {} endpoint hit", noteId, projectId);

            NoteDto updatedNote = projectNoteService.toggleNoteFavorite(projectId, noteId, null);

            return ResponseEntity.ok(
                    APIResponse.success(HttpStatus.OK.value(), "Note favorite status updated", updatedNote));
        } catch (RuntimeException e) {
            log.error("Error toggling favorite for note {} in project {}: {}", noteId, projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
        } catch (Exception e) {
            log.error(
                    "Unexpected error toggling favorite for note {} in project {}: {}",
                    noteId,
                    projectId,
                    e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to update note favorite status", null));
        }
    }

    /**
     * Get favorite notes for a project
     */
    @GetMapping("/favorites")
    public ResponseEntity<APIResponse<List<NoteDto>>> getFavoriteNotes(@PathVariable UUID projectId) {
        try {
            log.info("Get favorite notes for project {} endpoint hit", projectId);

            List<NoteDto> notes = projectNoteService.getFavoriteNotesByProjectId(projectId, null);

            return ResponseEntity.ok(
                    APIResponse.success(HttpStatus.OK.value(), "Favorite notes retrieved successfully", notes));
        } catch (RuntimeException e) {
            log.error("Error retrieving favorite notes for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error retrieving favorite notes for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve favorite notes", null));
        }
    }

    /**
     * Search notes by tag
     */
    @GetMapping("/search/tag")
    public ResponseEntity<APIResponse<List<NoteDto>>> searchNotesByTag(
            @PathVariable UUID projectId, @RequestParam String tag) {
        try {
            log.info("Search notes by tag {} for project {} endpoint hit", tag, projectId);

            List<NoteDto> notes = projectNoteService.searchNotesByTag(projectId, tag, null);

            return ResponseEntity.ok(
                    APIResponse.success(HttpStatus.OK.value(), "Notes search completed successfully", notes));
        } catch (RuntimeException e) {
            log.error("Error searching notes by tag {} for project {}: {}", tag, projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error searching notes by tag {} for project {}: {}", tag, projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to search notes", null));
        }
    }

    /**
     * Search notes by content
     */
    @GetMapping("/search/content")
    public ResponseEntity<APIResponse<List<NoteDto>>> searchNotesByContent(
            @PathVariable UUID projectId, @RequestParam String q) {
        try {
            log.info("Search notes by content {} for project {} endpoint hit", q, projectId);

            List<NoteDto> notes = projectNoteService.searchNotesByContent(projectId, q, null);

            return ResponseEntity.ok(
                    APIResponse.success(HttpStatus.OK.value(), "Notes search completed successfully", notes));
        } catch (RuntimeException e) {
            log.error("Error searching notes by content {} for project {}: {}", q, projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
        } catch (Exception e) {
            log.error(
                    "Unexpected error searching notes by content {} for project {}: {}", q, projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to search notes", null));
        }
    }

    /**
     * Upload an image for notes
     */
    @PostMapping("/images")
    public ResponseEntity<APIResponse<ImageUploadDto>> uploadImage(
            @PathVariable UUID projectId, @RequestParam("file") MultipartFile file) {
        try {
            log.info("Upload image for project {} endpoint hit", projectId);

            ImageUploadDto uploadedImage = noteImageService.uploadImage(projectId, file);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(APIResponse.success(
                            HttpStatus.CREATED.value(), "Image uploaded successfully", uploadedImage));
        } catch (RuntimeException e) {
            log.error("Error uploading image for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error uploading image for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to upload image", null));
        }
    }

    /**
     * Get image by ID
     */
    @GetMapping("/images/{imageId}")
    public ResponseEntity<Resource> getImage(@PathVariable UUID projectId, @PathVariable UUID imageId) {
        try {
            log.info("Get image {} for project {} endpoint hit", imageId, projectId);

            NoteImage image = noteImageService.getImageById(imageId);

            // Verify the image belongs to the project
            if (!image.getProjectId().equals(projectId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Path imagePath = Paths.get(image.getFilePath());
            if (!Files.exists(imagePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            byte[] imageBytes = Files.readAllBytes(imagePath);
            ByteArrayResource resource = new ByteArrayResource(imageBytes);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(image.getMimeType()));
            headers.setContentLength(imageBytes.length);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.getOriginalFilename() + "\"");

            return ResponseEntity.ok().headers(headers).body(resource);

        } catch (IOException e) {
            log.error("Error reading image file {} for project {}: {}", imageId, projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (RuntimeException e) {
            log.error("Error retrieving image {} for project {}: {}", imageId, projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Unexpected error retrieving image {} for project {}: {}", imageId, projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all images for a project
     */
    @GetMapping("/images")
    public ResponseEntity<APIResponse<List<ImageUploadDto>>> getImages(@PathVariable UUID projectId) {
        try {
            log.info("Get images for project {} endpoint hit", projectId);

            List<ImageUploadDto> images = noteImageService.getImagesByProjectId(projectId);

            return ResponseEntity.ok(
                    APIResponse.success(HttpStatus.OK.value(), "Images retrieved successfully", images));
        } catch (RuntimeException e) {
            log.error("Error retrieving images for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error retrieving images for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve images", null));
        }
    }

    /**
     * Delete an image
     */
    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<APIResponse<String>> deleteImage(@PathVariable UUID projectId, @PathVariable UUID imageId) {
        try {
            log.info("Delete image {} for project {} endpoint hit", imageId, projectId);

            NoteImage image = noteImageService.getImageById(imageId);

            // Verify the image belongs to the project
            if (!image.getProjectId().equals(projectId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(APIResponse.error(HttpStatus.FORBIDDEN.value(), "Image not found in project", null));
            }

            noteImageService.deleteImage(imageId);

            return ResponseEntity.ok(APIResponse.success(HttpStatus.OK.value(), "Image deleted successfully", null));
        } catch (RuntimeException e) {
            log.error("Error deleting image {} for project {}: {}", imageId, projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error deleting image {} for project {}: {}", imageId, projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to delete image", null));
        }
    }

    /**
     * Search papers for @ mention suggestions
     */
    @GetMapping("/papers/search")
    public ResponseEntity<APIResponse<List<PaperSuggestionDto>>> searchPapersForMention(
            @PathVariable UUID projectId, @RequestParam String q) {
        try {
            log.info("Search papers for mention in project {} with query: {}", projectId, q);

            List<PaperSuggestionDto> suggestions = paperMentionService.searchPapersForMention(projectId, q);

            return ResponseEntity.ok(APIResponse.success(
                    HttpStatus.OK.value(), "Paper suggestions retrieved successfully", suggestions));
        } catch (RuntimeException e) {
            log.error("Error searching papers for mention in project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error searching papers for mention in project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to search papers", null));
        }
    }

    /**
     * Generate AI content for notes
     */
    @PostMapping("/ai/generate")
    public ResponseEntity<APIResponse<AIContentResponseDto>> generateAIContent(
            @PathVariable UUID projectId, @Valid @RequestBody AIContentRequestDto request) {
        try {
            log.info("Generate AI content for project {} with prompt: {}", projectId, request.prompt());

            String generatedContent =
                    aiContentService.generateNoteContent(request.prompt(), request.context(), projectId);

            AIContentResponseDto response = new AIContentResponseDto(generatedContent, "success", null);

            return ResponseEntity.ok(
                    APIResponse.success(HttpStatus.OK.value(), "AI content generated successfully", response));
        } catch (RuntimeException e) {
            log.error("Error generating AI content for project {}: {}", projectId, e.getMessage());
            AIContentResponseDto errorResponse = new AIContentResponseDto(null, "error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage(), errorResponse));
        } catch (Exception e) {
            log.error("Unexpected error generating AI content for project {}: {}", projectId, e.getMessage());
            AIContentResponseDto errorResponse =
                    new AIContentResponseDto(null, "error", "Failed to generate AI content");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to generate AI content", errorResponse));
        }
    }
}
