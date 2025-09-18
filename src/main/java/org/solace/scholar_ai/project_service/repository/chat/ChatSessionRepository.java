package org.solace.scholar_ai.project_service.repository.chat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.solace.scholar_ai.project_service.model.chat.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    /**
     * Find active sessions for a specific paper
     */
    List<ChatSession> findByPaperIdAndIsActiveTrueOrderByLastMessageAtDesc(UUID paperId);

    /**
     * Find sessions by user ID (if authentication is implemented)
     */
    List<ChatSession> findByUserIdAndIsActiveTrueOrderByLastMessageAtDesc(String userId);

    /**
     * Find the most recent session for a paper
     */
    Optional<ChatSession> findFirstByPaperIdAndIsActiveTrueOrderByLastMessageAtDesc(UUID paperId);

    /**
     * Count active sessions for a paper
     */
    @Query("SELECT COUNT(cs) FROM ChatSession cs WHERE cs.paperId = :paperId AND cs.isActive = true")
    long countActiveSessionsForPaper(@Param("paperId") UUID paperId);

    /**
     * Find sessions that haven't been active for a certain period (for cleanup)
     */
    @Query("SELECT cs FROM ChatSession cs WHERE cs.lastMessageAt < :cutoffTime AND cs.isActive = true")
    List<ChatSession> findInactiveSessions(@Param("cutoffTime") Instant cutoffTime);

    /**
     * Update last message timestamp
     */
    @Query(
            "UPDATE ChatSession cs SET cs.lastMessageAt = :timestamp, cs.updatedAt = :timestamp WHERE cs.id = :sessionId")
    void updateLastMessageAt(@Param("sessionId") UUID sessionId, @Param("timestamp") Instant timestamp);

    /**
     * Find chat session IDs by project ID
     */
    @Query(
            "SELECT cs.id FROM ChatSession cs JOIN Paper p ON cs.paperId = p.id JOIN WebSearchOperation wso ON p.correlationId = wso.correlationId WHERE wso.projectId = :projectId")
    List<UUID> findIdsByProjectId(@Param("projectId") UUID projectId);

    /**
     * Delete chat sessions by project ID
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "DELETE FROM ChatSession cs WHERE cs.paperId IN (SELECT p.id FROM Paper p JOIN WebSearchOperation wso ON p.correlationId = wso.correlationId WHERE wso.projectId = :projectId)")
    void deleteByProjectId(@Param("projectId") UUID projectId);
}
