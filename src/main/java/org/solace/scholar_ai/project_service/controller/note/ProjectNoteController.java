package org.solace.scholar_ai.project_service.controller.note;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.note.CreateNoteDto;
import org.solace.scholar_ai.project_service.dto.note.NoteDto;
import org.solace.scholar_ai.project_service.dto.note.UpdateNoteDto;
import org.solace.scholar_ai.project_service.dto.response.APIResponse;
import org.solace.scholar_ai.project_service.service.note.ProjectNoteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("api/v1/projects/{projectId}/notes")
@RequiredArgsConstructor
public class ProjectNoteController {

    private final ProjectNoteService projectNoteService;

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
}
