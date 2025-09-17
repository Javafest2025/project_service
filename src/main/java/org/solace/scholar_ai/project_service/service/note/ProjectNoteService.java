package org.solace.scholar_ai.project_service.service.note;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.note.CreateNoteDto;
import org.solace.scholar_ai.project_service.dto.note.NoteDto;
import org.solace.scholar_ai.project_service.dto.note.UpdateNoteDto;
import org.solace.scholar_ai.project_service.mapping.note.ProjectNoteMapper;
import org.solace.scholar_ai.project_service.model.note.ProjectNote;
import org.solace.scholar_ai.project_service.repository.note.ProjectNoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "transactionManager")
public class ProjectNoteService {

    private static final String NOTE_NOT_FOUND_MESSAGE = "Note not found";

    private final ProjectNoteRepository projectNoteRepository;
    private final ProjectNoteMapper projectNoteMapper;
    private final NoteImageService noteImageService;
    private final PaperMentionService paperMentionService;

    /**
     * Get all notes for a project
     */
    @Transactional(readOnly = true, transactionManager = "transactionManager")
    public List<NoteDto> getNotesByProjectId(UUID projectId, UUID userId) {
        log.info("Fetching notes for project: {} for user: {}", projectId, userId);

        List<ProjectNote> notes = projectNoteRepository.findByProjectIdOrderByUpdatedAtDesc(projectId);
        return notes.stream().map(projectNoteMapper::toDto).toList();
    }

    /**
     * Get a specific note by ID
     */
    @Transactional(readOnly = true, transactionManager = "transactionManager")
    public NoteDto getNoteById(UUID projectId, UUID noteId, UUID userId) {
        log.info("Fetching note: {} for project: {} for user: {}", noteId, projectId, userId);

        ProjectNote note = projectNoteRepository
                .findByIdAndProjectId(noteId, projectId)
                .orElseThrow(() -> new RuntimeException(NOTE_NOT_FOUND_MESSAGE));

        return projectNoteMapper.toDto(note);
    }

    /**
     * Create a new note
     */
    public NoteDto createNote(UUID projectId, CreateNoteDto createNoteDto, UUID userId) {
        log.info("Creating new note for project: {} for user: {}", projectId, userId);

        ProjectNote note = projectNoteMapper.fromCreateDto(createNoteDto, projectId);
        ProjectNote savedNote = projectNoteRepository.save(note);

        // Associate any orphaned images with this note
        try {
            noteImageService.associateOrphanedImagesWithNote(projectId, savedNote.getId());
        } catch (Exception e) {
            log.warn("Failed to associate orphaned images with note {}: {}", savedNote.getId(), e.getMessage());
            // Continue with note creation even if image association fails
        }

        // Extract and save paper mentions
        try {
            paperMentionService.extractAndSaveMentions(projectId, savedNote.getId(), savedNote.getContent());
        } catch (Exception e) {
            log.warn("Failed to extract paper mentions from note {}: {}", savedNote.getId(), e.getMessage());
            // Continue with note creation even if mention extraction fails
        }

        log.info("Note created successfully with ID: {}", savedNote.getId());
        return projectNoteMapper.toDto(savedNote);
    }

    /**
     * Update an existing note
     */
    public NoteDto updateNote(UUID projectId, UUID noteId, UpdateNoteDto updateNoteDto, UUID userId) {
        log.info("Updating note: {} for project: {} for user: {}", noteId, projectId, userId);

        ProjectNote existingNote = projectNoteRepository
                .findByIdAndProjectId(noteId, projectId)
                .orElseThrow(() -> new RuntimeException(NOTE_NOT_FOUND_MESSAGE));

        // Update fields if provided
        if (updateNoteDto.title() != null) {
            existingNote.setTitle(updateNoteDto.title());
        }
        if (updateNoteDto.content() != null) {
            existingNote.setContent(updateNoteDto.content());
        }
        if (updateNoteDto.tags() != null) {
            existingNote.setTags(updateNoteDto.tags());
        }

        ProjectNote savedNote = projectNoteRepository.save(existingNote);

        // Associate any orphaned images with this note (in case new images were
        // uploaded)
        try {
            noteImageService.associateOrphanedImagesWithNote(projectId, savedNote.getId());
        } catch (Exception e) {
            log.warn("Failed to associate orphaned images with note {}: {}", savedNote.getId(), e.getMessage());
            // Continue with note update even if image association fails
        }

        // Extract and save paper mentions (in case content was updated)
        try {
            paperMentionService.extractAndSaveMentions(projectId, savedNote.getId(), savedNote.getContent());
        } catch (Exception e) {
            log.warn("Failed to extract paper mentions from note {}: {}", savedNote.getId(), e.getMessage());
            // Continue with note update even if mention extraction fails
        }

        log.info("Note updated successfully with ID: {}", savedNote.getId());
        return projectNoteMapper.toDto(savedNote);
    }

    /**
     * Delete a note
     */
    public void deleteNote(UUID projectId, UUID noteId, UUID userId) {
        log.info("Deleting note: {} for project: {} for user: {}", noteId, projectId, userId);

        ProjectNote note = projectNoteRepository
                .findByIdAndProjectId(noteId, projectId)
                .orElseThrow(() -> new RuntimeException(NOTE_NOT_FOUND_MESSAGE));

        // Delete associated images first
        try {
            noteImageService.deleteImagesByNoteId(noteId);
            log.info("Deleted associated images for note: {}", noteId);
        } catch (Exception e) {
            log.warn("Failed to delete some images for note {}: {}", noteId, e.getMessage());
            // Continue with note deletion even if image cleanup fails
        }

        // Delete associated paper mentions
        try {
            paperMentionService.deleteMentionsByNoteId(noteId);
            log.info("Deleted associated paper mentions for note: {}", noteId);
        } catch (Exception e) {
            log.warn("Failed to delete some paper mentions for note {}: {}", noteId, e.getMessage());
            // Continue with note deletion even if mention cleanup fails
        }

        // Delete the note
        projectNoteRepository.delete(note);

        log.info("Note deleted successfully with ID: {}", noteId);
    }

    /**
     * Toggle favorite status of a note
     */
    public NoteDto toggleNoteFavorite(UUID projectId, UUID noteId, UUID userId) {
        log.info("Toggling favorite status for note: {} in project: {} for user: {}", noteId, projectId, userId);

        ProjectNote note = projectNoteRepository
                .findByIdAndProjectId(noteId, projectId)
                .orElseThrow(() -> new RuntimeException(NOTE_NOT_FOUND_MESSAGE));

        note.setIsFavorite(!note.getIsFavorite());
        ProjectNote savedNote = projectNoteRepository.save(note);

        log.info("Note favorite status updated successfully with ID: {}", savedNote.getId());
        return projectNoteMapper.toDto(savedNote);
    }

    /**
     * Get favorite notes for a project
     */
    @Transactional(readOnly = true, transactionManager = "transactionManager")
    public List<NoteDto> getFavoriteNotesByProjectId(UUID projectId, UUID userId) {
        log.info("Fetching favorite notes for project: {} for user: {}", projectId, userId);

        List<ProjectNote> notes =
                projectNoteRepository.findByProjectIdAndIsFavoriteOrderByUpdatedAtDesc(projectId, true);
        return notes.stream().map(projectNoteMapper::toDto).toList();
    }

    /**
     * Search notes by tag
     */
    @Transactional(readOnly = true, transactionManager = "transactionManager")
    public List<NoteDto> searchNotesByTag(UUID projectId, String tag, UUID userId) {
        log.info("Searching notes by tag: {} for project: {} for user: {}", tag, projectId, userId);

        List<ProjectNote> notes = projectNoteRepository.findByProjectIdAndTag(projectId, tag);
        return notes.stream().map(projectNoteMapper::toDto).toList();
    }

    /**
     * Search notes by content
     */
    @Transactional(readOnly = true, transactionManager = "transactionManager")
    public List<NoteDto> searchNotesByContent(UUID projectId, String searchTerm, UUID userId) {
        log.info("Searching notes by content: {} for project: {} for user: {}", searchTerm, projectId, userId);

        List<ProjectNote> notes = projectNoteRepository.findByProjectIdAndSearchTerm(projectId, searchTerm);
        return notes.stream().map(projectNoteMapper::toDto).toList();
    }
}
