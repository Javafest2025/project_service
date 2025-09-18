package org.solace.scholar_ai.project_service.repository.summary;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.solace.scholar_ai.project_service.model.summary.PaperSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PaperSummaryRepository extends JpaRepository<PaperSummary, UUID> {

    Optional<PaperSummary> findByPaperId(UUID paperId);

    @Query("SELECT ps FROM PaperSummary ps WHERE ps.validationStatus = 'PENDING'")
    List<PaperSummary> findPendingValidation();

    @Query("SELECT ps FROM PaperSummary ps WHERE ps.confidence < :threshold")
    List<PaperSummary> findLowConfidenceSummaries(Double threshold);

    @Query("SELECT ps FROM PaperSummary ps WHERE ps.reproScore >= :minScore")
    List<PaperSummary> findHighReproducibilitySummaries(Double minScore);

    // Count summaries by paper IDs
    long countByPaperIdIn(List<UUID> paperIds);

    // Delete summaries by paper IDs
    void deleteByPaperIdIn(List<UUID> paperIds);
}
