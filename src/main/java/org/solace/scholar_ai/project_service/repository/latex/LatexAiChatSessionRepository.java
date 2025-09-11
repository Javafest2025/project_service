package org.solace.scholar_ai.project_service.repository.latex;

import org.solace.scholar_ai.project_service.model.latex.LatexAiChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LatexAiChatSessionRepository extends JpaRepository<LatexAiChatSession, Long> {

    /**
     * Find chat session by document ID
     */
    Optional<LatexAiChatSession> findByDocumentId(Long documentId);

    /**
     * Find all chat sessions for a project
     */
    List<LatexAiChatSession> findByProjectIdAndIsActiveTrue(Long projectId);

    /**
     * Find chat session by document ID and project ID
     */
    Optional<LatexAiChatSession> findByDocumentIdAndProjectId(Long documentId, Long projectId);

    /**
     * Check if a chat session exists for a document
     */
    boolean existsByDocumentId(Long documentId);

    /**
     * Get chat session with messages loaded
     */
    @Query("SELECT s FROM LatexAiChatSession s LEFT JOIN FETCH s.messages WHERE s.documentId = :documentId")
    Optional<LatexAiChatSession> findByDocumentIdWithMessages(@Param("documentId") Long documentId);

    /**
     * Get chat session with messages and checkpoints loaded
     */
    @Query("SELECT s FROM LatexAiChatSession s " +
           "LEFT JOIN FETCH s.messages m " +
           "LEFT JOIN FETCH s.checkpoints c " +
           "WHERE s.documentId = :documentId")
    Optional<LatexAiChatSession> findByDocumentIdWithMessagesAndCheckpoints(@Param("documentId") Long documentId);

    /**
     * Get active sessions count for a project
     */
    @Query("SELECT COUNT(s) FROM LatexAiChatSession s WHERE s.projectId = :projectId AND s.isActive = true")
    long countActiveSessionsByProject(@Param("projectId") Long projectId);
}
