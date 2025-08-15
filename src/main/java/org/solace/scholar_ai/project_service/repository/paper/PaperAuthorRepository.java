package org.solace.scholar_ai.project_service.repository.paper;

import java.util.List;
import java.util.UUID;
import org.solace.scholar_ai.project_service.model.paper.PaperAuthor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaperAuthorRepository extends JpaRepository<PaperAuthor, UUID> {

    // Find all paper-author relationships for a specific paper
    List<PaperAuthor> findByPaperId(UUID paperId);

    // Find all paper-author relationships for a specific author
    List<PaperAuthor> findByAuthorId(UUID authorId);

    // Find specific paper-author relationship
    PaperAuthor findByPaperIdAndAuthorId(UUID paperId, UUID authorId);

    // Find authors for a paper ordered by author order
    List<PaperAuthor> findByPaperIdOrderByAuthorOrderAsc(UUID paperId);

    // Find corresponding authors for a paper
    List<PaperAuthor> findByPaperIdAndIsCorrespondingAuthorTrue(UUID paperId);

    // Find papers by author with pagination
    @Query("SELECT pa FROM PaperAuthor pa WHERE pa.author.id = :authorId")
    List<PaperAuthor> findPapersByAuthor(@Param("authorId") UUID authorId);

    // Find authors by paper with pagination
    @Query("SELECT pa FROM PaperAuthor pa WHERE pa.paper.id = :paperId")
    List<PaperAuthor> findAuthorsByPaper(@Param("paperId") UUID paperId);

    // Delete all relationships for a paper
    void deleteByPaperId(UUID paperId);

    // Delete all relationships for an author
    void deleteByAuthorId(UUID authorId);

    // Check if relationship exists
    boolean existsByPaperIdAndAuthorId(UUID paperId, UUID authorId);
}
