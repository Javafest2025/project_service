package org.solace.scholar_ai.project_service.repository.latex;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.solace.scholar_ai.project_service.model.latex.LatexAiChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LatexAiChatSessionRepository extends JpaRepository<LatexAiChatSession, UUID> {

    /**
     * Find chat session by document ID
     */
    Optional<LatexAiChatSession> findByDocument_Id(UUID documentId);

    /**
     * Find all chat sessions for a project
     */
    List<LatexAiChatSession> findByProjectIdAndIsActiveTrue(UUID projectId);

    /**
     * Find chat session by document ID and project ID
     */
    Optional<LatexAiChatSession> findByDocument_IdAndProjectId(UUID documentId, UUID projectId);

    /**
     * Check if a chat session exists for a document
     */
    boolean existsByDocument_Id(UUID documentId);

    /**
     * Get chat session with messages loaded
     */
    @Query("SELECT s FROM LatexAiChatSession s LEFT JOIN FETCH s.messages WHERE s.document.id = :documentId")
    Optional<LatexAiChatSession> findByDocumentIdWithMessages(@Param("documentId") UUID documentId);

    /**
     * Get chat session with messages and checkpoints loaded
     * Note: Using separate queries to avoid MultipleBagFetchException
     */
    @Query("SELECT s FROM LatexAiChatSession s LEFT JOIN FETCH s.messages WHERE s.document.id = :documentId")
    Optional<LatexAiChatSession> findByDocumentIdWithMessagesAndCheckpoints(@Param("documentId") UUID documentId);

    /**
     * Get active sessions count for a project
     */
    @Query("SELECT COUNT(s) FROM LatexAiChatSession s WHERE s.projectId = :projectId AND s.isActive = true")
    long countActiveSessionsByProject(@Param("projectId") UUID projectId);

    /**
     * Delete sessions by document IDs
     */
    @Modifying
    @Query("DELETE FROM LatexAiChatSession s WHERE s.document.id IN :documentIds")
    void deleteByDocumentIdIn(@Param("documentIds") List<UUID> documentIds);
}
