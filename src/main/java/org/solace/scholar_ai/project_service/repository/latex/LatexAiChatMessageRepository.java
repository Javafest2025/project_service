package org.solace.scholar_ai.project_service.repository.latex;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.solace.scholar_ai.project_service.model.latex.LatexAiChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LatexAiChatMessageRepository extends JpaRepository<LatexAiChatMessage, Long> {

    /**
     * Find all messages for a session, ordered by creation time
     */
    @Query("SELECT m FROM LatexAiChatMessage m WHERE m.session.id = :sessionId ORDER BY m.createdAt ASC")
    List<LatexAiChatMessage> findBySessionIdOrderByCreatedAtAsc(@Param("sessionId") UUID sessionId);

    /**
     * Find messages by session and message type
     */
    @Query(
            "SELECT m FROM LatexAiChatMessage m WHERE m.session.id = :sessionId AND m.messageType = :messageType ORDER BY m.createdAt ASC")
    List<LatexAiChatMessage> findBySessionIdAndMessageTypeOrderByCreatedAtAsc(
            @Param("sessionId") UUID sessionId, @Param("messageType") LatexAiChatMessage.MessageType messageType);

    /**
     * Find unapplied AI messages with LaTeX suggestions
     */
    @Query("SELECT m FROM LatexAiChatMessage m WHERE m.session.id = :sessionId "
            + "AND m.messageType = 'AI' AND m.latexSuggestion IS NOT NULL "
            + "AND m.isApplied = false ORDER BY m.createdAt ASC")
    List<LatexAiChatMessage> findUnappliedAiSuggestions(@Param("sessionId") UUID sessionId);

    /**
     * Find recent messages (last N messages)
     */
    @Query("SELECT m FROM LatexAiChatMessage m WHERE m.session.id = :sessionId ORDER BY m.createdAt DESC")
    List<LatexAiChatMessage> findRecentMessages(
            @Param("sessionId") UUID sessionId, org.springframework.data.domain.Pageable pageable);

    /**
     * Count messages in a session
     */
    @Query("SELECT COUNT(m) FROM LatexAiChatMessage m WHERE m.session.id = :sessionId")
    long countBySessionId(@Param("sessionId") UUID sessionId);

    /**
     * Find messages created after a specific time
     */
    @Query(
            "SELECT m FROM LatexAiChatMessage m WHERE m.session.id = :sessionId AND m.createdAt > :afterTime ORDER BY m.createdAt ASC")
    List<LatexAiChatMessage> findBySessionIdAndCreatedAtAfterOrderByCreatedAtAsc(
            @Param("sessionId") UUID sessionId, @Param("afterTime") LocalDateTime afterTime);

    /**
     * Find AI messages with suggestions that were applied
     */
    @Query("SELECT m FROM LatexAiChatMessage m WHERE m.session.id = :sessionId "
            + "AND m.messageType = 'AI' AND m.latexSuggestion IS NOT NULL "
            + "AND m.isApplied = true ORDER BY m.createdAt DESC")
    List<LatexAiChatMessage> findAppliedAiSuggestions(@Param("sessionId") UUID sessionId);

    /**
     * Delete messages by document IDs
     */
    @Modifying
    @Query("DELETE FROM LatexAiChatMessage m WHERE m.session.document.id IN :documentIds")
    void deleteByDocumentIdIn(@Param("documentIds") List<UUID> documentIds);
}
