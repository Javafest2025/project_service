package org.solace.scholar_ai.project_service.service.paper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.paper.PaperMetadataDto;
import org.solace.scholar_ai.project_service.model.paper.Paper;
import org.solace.scholar_ai.project_service.repository.paper.PaperRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaperDeduplicationService {

    private final PaperRepository paperRepository;

    /**
     * Checks if a paper already exists in the database based on DOI or external
     * IDs.
     *
     * @param paperDto The paper DTO to check for duplicates
     * @return Optional containing the existing paper if found, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<Paper> findExistingPaper(PaperMetadataDto paperDto) {
        // 1. Primary check: DOI (most reliable identifier)
        if (StringUtils.hasText(paperDto.doi())) {
            log.debug("Checking for existing paper with DOI: {}", paperDto.doi());
            Optional<Paper> existingPaper = paperRepository.findFirstByDoi(paperDto.doi());
            if (existingPaper.isPresent()) {
                log.debug(
                        "Found existing paper with DOI: {} - Title: {}",
                        paperDto.doi(),
                        existingPaper.get().getTitle());
                return existingPaper;
            }
        }

        // 2. Secondary check: Semantic Scholar ID
        if (StringUtils.hasText(paperDto.semanticScholarId())) {
            log.debug("Checking for existing paper with Semantic Scholar ID: {}", paperDto.semanticScholarId());
            Optional<Paper> existingPaper = paperRepository.findFirstBySemanticScholarId(paperDto.semanticScholarId());
            if (existingPaper.isPresent()) {
                log.debug(
                        "Found existing paper with Semantic Scholar ID: {} - Title: {}",
                        paperDto.semanticScholarId(),
                        existingPaper.get().getTitle());
                return existingPaper;
            }
        }

        // 3. Tertiary check: External IDs (arXiv ID, PubMed ID, etc.)
        if (paperDto.externalIds() != null && !paperDto.externalIds().isEmpty()) {
            for (Map.Entry<String, Object> entry : paperDto.externalIds().entrySet()) {
                String source = entry.getKey();
                String value = entry.getValue() != null ? entry.getValue().toString() : null;

                if (StringUtils.hasText(value) && isReliableExternalIdSource(source)) {
                    log.debug("Checking for existing paper with external ID: {}={}", source, value);
                    Optional<Paper> existingPaper = paperRepository.findByExternalId(source, value);
                    if (existingPaper.isPresent()) {
                        log.debug(
                                "Found existing paper with external ID {}={} - Title: {}",
                                source,
                                value,
                                existingPaper.get().getTitle());
                        return existingPaper;
                    }
                }
            }
        }

        log.debug("No existing paper found for: {}", paperDto.title());
        return Optional.empty();
    }

    /**
     * Determines if an external ID source is reliable enough for deduplication.
     *
     * @param source The external ID source
     * @return true if the source is reliable for deduplication
     */
    private boolean isReliableExternalIdSource(String source) {
        if (!StringUtils.hasText(source)) {
            return false;
        }

        String sourceLower = source.toLowerCase();
        return sourceLower.equals("arxiv")
                || sourceLower.equals("pubmed")
                || sourceLower.equals("pmid")
                || sourceLower.equals("pmcid")
                || sourceLower.equals("isbn")
                || sourceLower.equals("issn")
                || sourceLower.equals("dblp")
                || sourceLower.equals("acm")
                || sourceLower.equals("ieee")
                || sourceLower.equals("scopus")
                || sourceLower.equals("wos")
                || // Web of Science
                sourceLower.equals("crossref");
    }

    /**
     * Filters out papers that already exist in the database.
     *
     * @param paperDtos List of paper DTOs to filter
     * @return List of papers that don't exist in the database
     */
    @Transactional(readOnly = true)
    public List<PaperMetadataDto> filterNewPapers(List<PaperMetadataDto> paperDtos) {
        return paperDtos.stream()
                .filter(dto -> {
                    Optional<Paper> existing = findExistingPaper(dto);
                    if (existing.isPresent()) {
                        log.info(
                                "Skipping duplicate paper: '{}' (DOI: {}) - Already exists with ID: {}",
                                dto.title(),
                                dto.doi(),
                                existing.get().getId());
                        return false;
                    }
                    return true;
                })
                .toList();
    }
}
