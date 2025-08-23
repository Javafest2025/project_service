package org.solace.scholar_ai.project_service.repository.author;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.solace.scholar_ai.project_service.model.author.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorRepository extends JpaRepository<Author, UUID> {

    // Basic search methods
    List<Author> findByNameContainingIgnoreCase(String name);

    Optional<Author> findByNameIgnoreCase(String name);

    Optional<Author> findByOrcidId(String orcidId);

    Optional<Author> findByEmail(String email);

    List<Author> findAllByOrderByNameAsc();

    List<Author> findByLastSyncAtBeforeOrLastSyncAtIsNull(Instant cutoff);

    List<Author> findByPrimaryAffiliationContainingIgnoreCase(String primaryAffiliation);

    // Advanced search with pagination
    Page<Author> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query(
            "SELECT a FROM Author a WHERE a.name LIKE %:keyword% OR a.primaryAffiliation LIKE %:keyword% OR a.allAffiliations LIKE %:keyword% OR a.researchAreas LIKE %:keyword%")
    Page<Author> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // Search by research areas
    @Query("SELECT a FROM Author a WHERE a.researchAreas LIKE %:field%")
    List<Author> findByResearchArea(@Param("field") String field);

    // Search by affiliation
    @Query("SELECT a FROM Author a WHERE a.allAffiliations LIKE %:affiliation%")
    List<Author> findByAffiliation(@Param("affiliation") String affiliation);

    // Find authors with high citation count
    List<Author> findByCitationCountGreaterThanOrderByCitationCountDesc(Integer minCitations);

    // Find authors by paper count
    List<Author> findByPaperCountGreaterThanOrderByPaperCountDesc(Integer minPaperCount);

    // Find authors by data quality score
    List<Author> findByDataQualityScoreGreaterThanOrderByDataQualityScoreDesc(Double minQualityScore);

    // Find authors by data source
    @Query("SELECT a FROM Author a WHERE a.dataSources LIKE %:source%")
    List<Author> findByDataSource(@Param("source") String source);
}
