package org.solace.scholar_ai.project_service.repository.citation;

import java.util.List;
import java.util.UUID;
import org.solace.scholar_ai.project_service.model.citation.CitationEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CitationEvidenceRepository extends JpaRepository<CitationEvidence, UUID> {

    /**
     * Find all evidence for a citation issue
     */
    List<CitationEvidence> findByCitationIssueIdOrderBySimilarityDesc(UUID issueId);

    /**
     * Find evidence by source kind (local/web)
     */
    @Query(
            "SELECT e FROM CitationEvidence e WHERE e.citationIssue.id = :issueId AND FUNCTION('jsonb_extract_path_text', e.source, 'kind') = :sourceKind ORDER BY e.similarity DESC")
    List<CitationEvidence> findByIssueIdAndSourceKind(
            @Param("issueId") UUID issueId, @Param("sourceKind") String sourceKind);

    /**
     * Find high-confidence evidence (similarity > threshold)
     */
    @Query(
            "SELECT e FROM CitationEvidence e WHERE e.citationIssue.id = :issueId AND e.similarity > :threshold ORDER BY e.similarity DESC")
    List<CitationEvidence> findHighConfidenceByIssueId(
            @Param("issueId") UUID issueId, @Param("threshold") Double threshold);

    /**
     * Find evidence supporting the citation
     */
    @Query(
            "SELECT e FROM CitationEvidence e WHERE e.citationIssue.id = :issueId AND e.supportScore > 0.5 ORDER BY e.supportScore DESC")
    List<CitationEvidence> findSupportingEvidenceByIssueId(@Param("issueId") UUID issueId);

    /**
     * Find evidence contradicting the citation
     */
    @Query(
            "SELECT e FROM CitationEvidence e WHERE e.citationIssue.id = :issueId AND e.supportScore < 0.5 ORDER BY e.supportScore ASC")
    List<CitationEvidence> findContradictingEvidenceByIssueId(@Param("issueId") UUID issueId);

    /**
     * Count evidence by source kind (local/web)
     */
    @Query(
            "SELECT COUNT(e) FROM CitationEvidence e WHERE e.citationIssue.id = :issueId AND FUNCTION('jsonb_extract_path_text', e.source, 'kind') = :sourceKind")
    Long countByIssueIdAndSourceKind(@Param("issueId") UUID issueId, @Param("sourceKind") String sourceKind);

    /**
     * Find best evidence for an issue (highest combined score)
     */
    @Query(
            "SELECT e FROM CitationEvidence e WHERE e.citationIssue.id = :issueId ORDER BY (e.similarity + e.supportScore) DESC")
    List<CitationEvidence> findBestEvidenceByIssueId(@Param("issueId") UUID issueId);

    /**
     * Delete evidence for a citation issue
     */
    void deleteByCitationIssueId(UUID issueId);

    /**
     * Find evidence by external reference ID
     */
    @Query("SELECT e FROM CitationEvidence e WHERE FUNCTION('jsonb_extract_path_text', e.source, 'paperId') = :paperId")
    List<CitationEvidence> findByPaperId(@Param("paperId") String paperId);

    /**
     * Find evidence containing specific text
     */
    @Query(
            "SELECT e FROM CitationEvidence e WHERE e.citationIssue.id = :issueId AND LOWER(e.matchedText) LIKE LOWER(CONCAT('%', :searchText, '%')) ORDER BY e.similarity DESC")
    List<CitationEvidence> findByIssueIdAndTextContaining(
            @Param("issueId") UUID issueId, @Param("searchText") String searchText);

    /**
     * Find evidence with minimum similarity score
     */
    @Query(
            "SELECT e FROM CitationEvidence e WHERE e.citationIssue.id = :issueId AND e.similarity >= :minScore ORDER BY e.similarity DESC")
    List<CitationEvidence> findByIssueIdAndMinSimilarityScore(
            @Param("issueId") UUID issueId, @Param("minScore") Double minScore);
}
