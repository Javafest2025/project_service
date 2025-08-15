package org.solace.scholar_ai.project_service.repository.author;

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

    Optional<Author> findByAuthorId(String authorId);

    Optional<Author> findByOrcid(String orcid);

    Optional<Author> findByEmail(String email);

    // Advanced search with pagination
    Page<Author> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query(
            "SELECT a FROM Author a WHERE a.name LIKE %:keyword% OR a.fieldsOfStudy LIKE %:keyword% OR a.affiliations LIKE %:keyword%")
    Page<Author> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // Search by fields of study
    @Query("SELECT a FROM Author a WHERE a.fieldsOfStudy LIKE %:field%")
    List<Author> findByFieldOfStudy(@Param("field") String field);

    // Search by affiliation
    @Query("SELECT a FROM Author a WHERE a.affiliations LIKE %:affiliation%")
    List<Author> findByAffiliation(@Param("affiliation") String affiliation);

    // Find authors with high citation count
    List<Author> findByCitationCountGreaterThanOrderByCitationCountDesc(Integer minCitations);

    // Find authors by paper count
    List<Author> findByPaperCountGreaterThanOrderByPaperCountDesc(Integer minPaperCount);

    // Find authors by confidence score
    List<Author> findByConfidenceScoreGreaterThanOrderByConfidenceScoreDesc(Double minConfidence);

    // Find authors by source
    @Query("SELECT a FROM Author a WHERE a.sources LIKE %:source%")
    List<Author> findBySource(@Param("source") String source);
}
