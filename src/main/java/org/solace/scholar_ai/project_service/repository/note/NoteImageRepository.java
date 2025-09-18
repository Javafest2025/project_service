package org.solace.scholar_ai.project_service.repository.note;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.solace.scholar_ai.project_service.model.note.NoteImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NoteImageRepository extends JpaRepository<NoteImage, UUID> {

    /**
     * Find all images for a specific project
     */
    List<NoteImage> findByProjectIdOrderByUploadedAtDesc(UUID projectId);

    /**
     * Find all images for a specific note
     */
    List<NoteImage> findByNoteIdOrderByUploadedAtDesc(UUID noteId);

    /**
     * Find image by stored filename
     */
    Optional<NoteImage> findByStoredFilename(String storedFilename);

    /**
     * Find images by project ID and note ID
     */
    @Query(
            "SELECT ni FROM NoteImage ni WHERE ni.projectId = :projectId AND ni.noteId = :noteId ORDER BY ni.uploadedAt DESC")
    List<NoteImage> findByProjectIdAndNoteId(@Param("projectId") UUID projectId, @Param("noteId") UUID noteId);

    /**
     * Delete images by project ID
     */
    void deleteByProjectId(UUID projectId);

    /**
     * Delete images by note ID
     */
    void deleteByNoteId(UUID noteId);

    /**
     * Count images by project ID
     */
    long countByProjectId(UUID projectId);
}
