package org.solace.scholar_ai.project_service.repository;

import java.util.Optional;
import org.solace.scholar_ai.project_service.model.paper.AbstractAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AbstractAnalysisRepository extends JpaRepository<AbstractAnalysis, String> {

    @Query(
            "SELECT aa FROM AbstractAnalysis aa WHERE aa.paperId = :paperId AND aa.isActive = true ORDER BY aa.updatedAt DESC")
    Optional<AbstractAnalysis> findLatestByPaperId(@Param("paperId") String paperId);

    @Query(
            "SELECT aa FROM AbstractAnalysis aa WHERE aa.paperId = :paperId AND aa.abstractTextHash = :abstractTextHash AND aa.isActive = true")
    Optional<AbstractAnalysis> findByPaperIdAndAbstractTextHash(
            @Param("paperId") String paperId, @Param("abstractTextHash") String abstractTextHash);

    @Query("SELECT COUNT(aa) > 0 FROM AbstractAnalysis aa WHERE aa.paperId = :paperId AND aa.isActive = true")
    boolean existsByPaperId(@Param("paperId") String paperId);

    @Query(
            "SELECT aa FROM AbstractAnalysis aa LEFT JOIN FETCH aa.highlights WHERE aa.paperId = :paperId AND aa.isActive = true ORDER BY aa.updatedAt DESC")
    Optional<AbstractAnalysis> findLatestByPaperIdWithHighlights(@Param("paperId") String paperId);
}
