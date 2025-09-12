package org.solace.scholar_ai.project_service.repository.latex;

import java.time.LocalDateTime;
import java.util.List;
import org.solace.scholar_ai.project_service.model.latex.LatexAiChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LatexAiChatMessageRepository extends JpaRepository<LatexAiChatMessage, Long> {

    /**
     * Find all messages for a session, ordered by creation time
     */
    List<LatexAiChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    /**
     * Find messages by session and message type
     */
    List<LatexAiChatMessage> findBySessionIdAndMessageTypeOrderByCreatedAtAsc(
            Long sessionId, LatexAiChatMessage.MessageType messageType);

    /**
     * Find unapplied AI messages with LaTeX suggestions
     */
    @Query("SELECT m FROM LatexAiChatMessage m WHERE m.session.id = :sessionId "
            + "AND m.messageType = 'AI' AND m.latexSuggestion IS NOT NULL "
            + "AND m.isApplied = false ORDER BY m.createdAt ASC")
    List<LatexAiChatMessage> findUnappliedAiSuggestions(@Param("sessionId") Long sessionId);

    /**
     * Find recent messages (last N messages)
     */
    @Query("SELECT m FROM LatexAiChatMessage m WHERE m.session.id = :sessionId "
            + "ORDER BY m.createdAt DESC LIMIT :limit")
    List<LatexAiChatMessage> findRecentMessages(@Param("sessionId") Long sessionId, @Param("limit") int limit);

    /**
     * Count messages in a session
     */
    long countBySessionId(Long sessionId);

    /**
     * Find messages created after a specific time
     */
    List<LatexAiChatMessage> findBySessionIdAndCreatedAtAfterOrderByCreatedAtAsc(
            Long sessionId, LocalDateTime afterTime);

    /**
     * Find AI messages with suggestions that were applied
     */
    @Query("SELECT m FROM LatexAiChatMessage m WHERE m.session.id = :sessionId "
            + "AND m.messageType = 'AI' AND m.latexSuggestion IS NOT NULL "
            + "AND m.isApplied = true ORDER BY m.createdAt DESC")
    List<LatexAiChatMessage> findAppliedAiSuggestions(@Param("sessionId") Long sessionId);
}
