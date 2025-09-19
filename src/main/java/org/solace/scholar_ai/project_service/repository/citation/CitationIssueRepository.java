package org.solace.scholar_ai.project_service.repository.citation;

import java.util.List;
import java.util.UUID;
import org.solace.scholar_ai.project_service.model.citation.CitationIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CitationIssueRepository extends JpaRepository<CitationIssue, UUID> {

    /**
     * Find all issues for a citation check
     */
    List<CitationIssue> findByCitationCheckIdOrderByFromPosAsc(UUID citationCheckId);

    /**
     * Find issues by type for a citation check
     */
    List<CitationIssue> findByCitationCheckIdAndTypeOrderByFromPosAsc(
            UUID citationCheckId, CitationIssue.IssueType issueType);

    /**
     * Find issues by severity for a citation check
     */
    List<CitationIssue> findByCitationCheckIdAndSeverityOrderByFromPosAsc(
            UUID citationCheckId, CitationIssue.Severity severity);

    /**
     * Find unresolved issues for a citation check
     */
    @Query(
            "SELECT i FROM CitationIssue i WHERE i.citationCheck.id = :citationCheckId AND i.resolved = false ORDER BY i.fromPos ASC")
    List<CitationIssue> findUnresolvedByCitationCheckId(@Param("citationCheckId") UUID citationCheckId);

    /**
     * Count issues by severity for a citation check
     */
    @Query(
            "SELECT COUNT(i) FROM CitationIssue i WHERE i.citationCheck.id = :citationCheckId AND i.severity = :severity")
    Long countByCitationCheckIdAndSeverity(
            @Param("citationCheckId") UUID citationCheckId, @Param("severity") CitationIssue.Severity severity);

    /**
     * Count unresolved issues for a citation check
     */
    @Query("SELECT COUNT(i) FROM CitationIssue i WHERE i.citationCheck.id = :citationCheckId AND i.resolved = false")
    Long countUnresolvedByCitationCheckId(@Param("citationCheckId") UUID citationCheckId);

    /**
     * Find issues with evidence loaded
     */
    @Query(
            "SELECT i FROM CitationIssue i LEFT JOIN FETCH i.evidence WHERE i.citationCheck.id = :citationCheckId ORDER BY i.fromPos ASC")
    List<CitationIssue> findByCitationCheckIdWithEvidence(@Param("citationCheckId") UUID citationCheckId);

    /**
     * Find issues for citation text search
     */
    @Query(
            "SELECT i FROM CitationIssue i WHERE i.citationCheck.id = :citationCheckId AND LOWER(i.snippet) LIKE LOWER(CONCAT('%', :searchText, '%')) ORDER BY i.fromPos ASC")
    List<CitationIssue> findByCitationCheckIdAndCitationTextContaining(
            @Param("citationCheckId") UUID citationCheckId, @Param("searchText") String searchText);

    /**
     * Mark all issues as resolved for a citation check
     */
    @Query("UPDATE CitationIssue i SET i.resolved = true WHERE i.citationCheck.id = :citationCheckId")
    void markAllResolvedByCitationCheckId(@Param("citationCheckId") UUID citationCheckId);

    /**
     * Find issues by document position range
     */
    @Query(
            "SELECT i FROM CitationIssue i WHERE i.citationCheck.id = :citationCheckId AND i.fromPos BETWEEN :startPos AND :endPos ORDER BY i.fromPos ASC")
    List<CitationIssue> findByCitationCheckIdAndPositionRange(
            @Param("citationCheckId") UUID citationCheckId,
            @Param("startPos") Integer startPos,
            @Param("endPos") Integer endPos);
}
