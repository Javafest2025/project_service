package org.solace.scholar_ai.project_service.repository.chat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.solace.scholar_ai.project_service.model.chat.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    /**
     * Find active sessions for a specific paper
     */
    List<ChatSession> findByPaperIdAndIsActiveTrueOrderByLastActiveDesc(UUID paperId);

    /**
     * Find sessions by user ID (if authentication is implemented)
     */
    List<ChatSession> findByUserIdAndIsActiveTrueOrderByLastActiveDesc(UUID userId);

    /**
     * Find the most recent session for a paper
     */
    Optional<ChatSession> findFirstByPaperIdAndIsActiveTrueOrderByLastActiveDesc(UUID paperId);

    /**
     * Count active sessions for a paper
     */
    @Query("SELECT COUNT(cs) FROM ChatSession cs WHERE cs.paperId = :paperId AND cs.isActive = true")
    long countActiveSessionsForPaper(@Param("paperId") UUID paperId);

    /**
     * Find sessions that haven't been active for a certain period (for cleanup)
     */
    @Query("SELECT cs FROM ChatSession cs WHERE cs.lastActive < :cutoffTime AND cs.isActive = true")
    List<ChatSession> findInactiveSessions(@Param("cutoffTime") Instant cutoffTime);

    /**
     * Update last active timestamp
     */
    @Query("UPDATE ChatSession cs SET cs.lastActive = :timestamp WHERE cs.id = :sessionId")
    void updateLastActive(@Param("sessionId") UUID sessionId, @Param("timestamp") Instant timestamp);
}
