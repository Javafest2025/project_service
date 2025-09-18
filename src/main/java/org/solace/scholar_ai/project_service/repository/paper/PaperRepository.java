package org.solace.scholar_ai.project_service.repository.paper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.solace.scholar_ai.project_service.model.paper.Paper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaperRepository extends JpaRepository<Paper, UUID> {

    List<Paper> findByCorrelationId(String correlationId);

    List<Paper> findByCorrelationIdIn(List<String> correlationIds);

    List<Paper> findByTitleContainingIgnoreCase(String title);

    @Query("SELECT p FROM Paper p JOIN p.paperAuthors pa JOIN pa.author a WHERE a.name LIKE %:author%")
    List<Paper> findByAuthorNameContainingIgnoreCase(@Param("author") String author);

    List<Paper> findByPublicationDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT p FROM Paper p WHERE p.title LIKE %:keyword% OR p.abstractText LIKE %:keyword%")
    List<Paper> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT p FROM Paper p JOIN p.venue v WHERE v.venueName LIKE %:venue%")
    List<Paper> findByVenueNameContainingIgnoreCase(@Param("venue") String venue);

    List<Paper> findByDoi(String doi);

    List<Paper> findBySemanticScholarId(String semanticScholarId);

    // New methods for deduplication
    Optional<Paper> findFirstByDoi(String doi);

    Optional<Paper> findFirstBySemanticScholarId(String semanticScholarId);

    @Query("SELECT p FROM Paper p JOIN p.externalIds e WHERE e.source = :source AND e.value = :value")
    Optional<Paper> findByExternalId(@Param("source") String source, @Param("value") String value);

    @Query("SELECT p FROM Paper p JOIN p.externalIds e WHERE e.source IN :sources AND e.value IN :values")
    List<Paper> findByExternalIds(@Param("sources") List<String> sources, @Param("values") List<String> values);

    // Search methods with pagination
    Page<Paper> findByTitleContainingIgnoreCaseOrAbstractTextContainingIgnoreCase(
            String titleQuery, String abstractQuery, Pageable pageable);

    // LaTeX Context methods
    List<Paper> findByCorrelationIdInAndIsLatexContext(List<String> correlationIds, Boolean isLatexContext);

    /**
     * Find paper IDs by project ID
     */
    @Query(
            "SELECT p.id FROM Paper p WHERE p.correlationId IN (SELECT wso.correlationId FROM WebSearchOperation wso WHERE wso.projectId = :projectId)")
    List<UUID> findIdsByProjectId(@Param("projectId") UUID projectId);

    /**
     * Delete papers by IDs
     */
    void deleteByIdIn(List<UUID> paperIds);
}
