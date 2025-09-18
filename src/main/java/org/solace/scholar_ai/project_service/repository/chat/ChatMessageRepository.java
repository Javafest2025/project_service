package org.solace.scholar_ai.project_service.repository.chat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.solace.scholar_ai.project_service.model.chat.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /**
     * Find all messages for a session ordered by timestamp
     */
    List<ChatMessage> findBySessionIdOrderByTimestampAsc(UUID sessionId);

    /**
     * Find recent messages for a session (for context)
     */
    List<ChatMessage> findBySessionIdOrderByTimestampDesc(UUID sessionId);

    /**
     * Find the most recent message in a session
     */
    java.util.Optional<ChatMessage> findFirstBySessionIdOrderByTimestampDesc(UUID sessionId);

    /**
     * Find messages within a time range
     */
    @Query(
            "SELECT cm FROM ChatMessage cm WHERE cm.sessionId = :sessionId AND cm.timestamp BETWEEN :startTime AND :endTime ORDER BY cm.timestamp ASC")
    List<ChatMessage> findMessagesInTimeRange(
            @Param("sessionId") UUID sessionId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Count messages in a session
     */
    long countBySessionId(UUID sessionId);

    /**
     * Find messages by role in a session
     */
    List<ChatMessage> findBySessionIdAndRoleOrderByTimestampDesc(UUID sessionId, ChatMessage.Role role);

    /**
     * Get the latest message in a session
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.sessionId = :sessionId ORDER BY cm.timestamp DESC LIMIT 1")
    ChatMessage findLatestMessageInSession(@Param("sessionId") UUID sessionId);

    /**
     * Delete old messages (for cleanup)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ChatMessage cm WHERE cm.timestamp < :cutoffTime")
    void deleteOldMessages(@Param("cutoffTime") Instant cutoffTime);

    /**
     * Find messages with token count for analysis
     */
    @Query(
            "SELECT cm FROM ChatMessage cm WHERE cm.sessionId = :sessionId AND cm.tokenCount IS NOT NULL ORDER BY cm.timestamp DESC")
    List<ChatMessage> findMessagesWithTokenCount(@Param("sessionId") UUID sessionId);

    /**
     * Delete chat messages by chat session IDs
     */
    void deleteBySessionIdIn(List<UUID> sessionIds);
}
