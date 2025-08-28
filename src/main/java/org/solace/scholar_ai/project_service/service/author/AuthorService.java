package org.solace.scholar_ai.project_service.service.author;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.author.AuthorDto;
import org.solace.scholar_ai.project_service.dto.author.AuthorSyncRequestDto;
import org.solace.scholar_ai.project_service.exception.CustomException;
import org.solace.scholar_ai.project_service.exception.ErrorCode;
import org.solace.scholar_ai.project_service.mapping.author.AuthorMapper;
import org.solace.scholar_ai.project_service.model.author.Author;
import org.solace.scholar_ai.project_service.model.paper.Paper;
import org.solace.scholar_ai.project_service.model.paper.PaperAuthor;
import org.solace.scholar_ai.project_service.repository.author.AuthorRepository;
import org.solace.scholar_ai.project_service.repository.paper.PaperAuthorRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorService {

    private final AuthorRepository authorRepository;
    private final PaperAuthorRepository paperAuthorRepository;
    private final AuthorMapper authorMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${scholarai.fastapi.papersearch-url}")
    private String paperSearchBaseUrl;

    private static final int CACHE_EXPIRY_HOURS = 24;

    /**
     * Fetch author information - smart method that returns cached data or syncs if
     * needed
     */
    @Transactional
    public AuthorDto fetchAuthor(String name, String strategy, String userId) {
        log.info("Fetching author: {} with strategy: {}", name, strategy);

        // Find existing author by name (now unique)
        Optional<Author> existingAuthor = authorRepository.findByNameIgnoreCase(name.trim());

        if (existingAuthor.isPresent()) {
            Author author = existingAuthor.get();

            // Check if data is sufficient and fresh
            if (isDataSufficientAndFresh(author)) {
                log.info("Using cached author data for: {}", name);
                return authorMapper.toDto(author);
            }
        }

        // Data is missing or insufficient, trigger sync
        log.info("Data insufficient or stale, triggering sync for: {}", name);
        AuthorSyncRequestDto syncRequest = new AuthorSyncRequestDto(userId, name, strategy, false);
        return syncAuthor(syncRequest);
    }

    /**
     * Check if author data is sufficient and fresh enough
     */
    private boolean isDataSufficientAndFresh(Author author) {
        // Check if synced recently
        boolean isFresh = author.getIsSynced() != null
                && author.getIsSynced()
                && author.getLastSyncAt() != null
                && author.getLastSyncAt().isAfter(Instant.now().minus(CACHE_EXPIRY_HOURS, ChronoUnit.HOURS));

        // Check if has comprehensive data
        boolean hasSufficientData = author.getSemanticScholarId() != null
                && author.getCitationCount() != null
                && author.getCitationCount() > 0;

        return isFresh && hasSufficientData;
    }

    /**
     * Fetch author information by ID from database
     */
    @Transactional(readOnly = true)
    public AuthorDto getAuthorById(UUID authorId) {
        log.info("Fetching author by ID: {}", authorId);

        Author author = authorRepository
                .findById(authorId)
                .orElseThrow(() -> new CustomException(
                        "Author not found with ID: " + authorId, HttpStatus.NOT_FOUND, ErrorCode.AUTHOR_NOT_FOUND));

        return authorMapper.toDto(author);
    }

    /**
     * Search for multiple authors by name pattern
     */
    @Transactional(readOnly = true)
    public List<AuthorDto> searchAuthors(String namePattern) {
        log.info("Searching authors with pattern: {}", namePattern);

        List<Author> authors = authorRepository.findByNameContainingIgnoreCase(namePattern.trim());

        return authors.stream().map(authorMapper::toDto).collect(Collectors.toList());
    }

    /**
     * Force sync author information from external multi-source paper-search service
     */
    @Transactional
    public AuthorDto syncAuthor(AuthorSyncRequestDto request) {
        log.info(
                "Force syncing author: {} with strategy: {} for user: {}",
                request.name(),
                request.strategy(),
                request.userId());

        // Find existing author or create new one
        Optional<Author> existingAuthor =
                authorRepository.findByNameIgnoreCase(request.name().trim());

        // Always fetch fresh data for sync (unless explicitly using cache)
        boolean shouldFetch = request.forceRefresh() == null || request.forceRefresh();

        if (existingAuthor.isPresent() && !shouldFetch) {
            Author author = existingAuthor.get();
            if (author.getIsSynced() != null
                    && author.getIsSynced()
                    && author.getLastSyncAt() != null
                    && author.getLastSyncAt().isAfter(Instant.now().minus(1, ChronoUnit.HOURS))) {
                log.info("Using recent sync data for: {}", request.name());
                return authorMapper.toDto(author);
            }
        }

        try {
            // Fetch fresh data from multi-source paper-search service
            Map<String, Object> apiResponse = fetchAuthorFromMultiSourceAPI(request.name(), request.strategy());

            // Save or update in database
            Author author = existingAuthor.orElse(new Author());
            updateAuthorFromMultiSourceResponse(author, apiResponse);
            author.setLastSyncAt(Instant.now());
            author.setIsSynced(true);
            author.setSyncError(null);

            Author savedAuthor = authorRepository.save(author);
            log.info(
                    "Successfully synced and saved author: {} with ID: {}", savedAuthor.getName(), savedAuthor.getId());

            return authorMapper.toDto(savedAuthor);

        } catch (Exception e) {
            log.error("Failed to sync author: {}", request.name(), e);

            // Update existing author with sync error if exists
            if (existingAuthor.isPresent()) {
                Author author = existingAuthor.get();
                author.setIsSynced(false);
                author.setSyncError(e.getMessage());
                author.setLastSyncAt(Instant.now());
                Author savedAuthor = authorRepository.save(author);
                return authorMapper.toDto(savedAuthor);
            }

            throw new CustomException(
                    "Failed to sync author from external sources: " + e.getMessage(),
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
    }

    /**
     * Get all authors from database
     */
    @Transactional(readOnly = true)
    public List<AuthorDto> getAllAuthors() {
        log.info("Fetching all authors");

        List<Author> authors = authorRepository.findAllByOrderByNameAsc();

        return authors.stream().map(authorMapper::toDto).collect(Collectors.toList());
    }

    /**
     * Get authors with stale data (for background refresh)
     */
    @Transactional(readOnly = true)
    public List<AuthorDto> getStaleAuthors(int hours) {
        log.info("Fetching authors with data older than {} hours", hours);

        Instant cutoff = Instant.now().minus(hours, ChronoUnit.HOURS);
        List<Author> staleAuthors = authorRepository.findByLastSyncAtBeforeOrLastSyncAtIsNull(cutoff);

        return staleAuthors.stream().map(authorMapper::toDto).collect(Collectors.toList());
    }

    /**
     * Get or create author and link to paper (handles many-to-many relationship)
     * This method ensures unique author names and proper paper-author relationships
     */
    @Transactional
    public Author getOrCreateAuthorForPaper(
            String authorName, Paper paper, Integer authorOrder, Boolean isCorrespondingAuthor) {
        log.info("Getting or creating author '{}' for paper '{}'", authorName, paper.getId());

        // Check if author already exists
        Optional<Author> existingAuthor = authorRepository.findByNameIgnoreCase(authorName.trim());

        Author author;
        if (existingAuthor.isPresent()) {
            author = existingAuthor.get();
            log.info("Found existing author: {} with ID: {}", author.getName(), author.getId());
        } else {
            // Create new author with basic information
            author = new Author();
            author.setName(authorName.trim());
            author.setIsSynced(false);
            author = authorRepository.save(author);
            log.info("Created new author: {} with ID: {}", author.getName(), author.getId());
        }

        // Check if paper-author relationship already exists
        if (!paperAuthorRepository.existsByPaperIdAndAuthorId(paper.getId(), author.getId())) {
            // Create paper-author relationship
            PaperAuthor paperAuthor = new PaperAuthor(paper, author, authorOrder);
            paperAuthor.setIsCorrespondingAuthor(isCorrespondingAuthor);
            paperAuthorRepository.save(paperAuthor);
            log.info(
                    "Created paper-author relationship for paper '{}' and author '{}'",
                    paper.getId(),
                    author.getName());
        } else {
            log.info(
                    "Paper-author relationship already exists for paper '{}' and author '{}'",
                    paper.getId(),
                    author.getName());
        }

        return author;
    }

    /**
     * Get all authors for a specific paper
     */
    @Transactional(readOnly = true)
    public List<AuthorDto> getAuthorsForPaper(UUID paperId) {
        log.info("Fetching authors for paper: {}", paperId);

        List<PaperAuthor> paperAuthors = paperAuthorRepository.findByPaperIdOrderByAuthorOrderAsc(paperId);

        return paperAuthors.stream()
                .map(pa -> authorMapper.toDto(pa.getAuthor()))
                .collect(Collectors.toList());
    }

    /**
     * Get all papers for a specific author
     */
    @Transactional(readOnly = true)
    public List<PaperAuthor> getPapersForAuthor(UUID authorId) {
        log.info("Fetching papers for author: {}", authorId);

        return paperAuthorRepository.findByAuthorId(authorId);
    }

    /**
     * Fetch author data from multi-source paper-search API
     */
    private Map<String, Object> fetchAuthorFromMultiSourceAPI(String authorName, String strategy) {
        log.info("Fetching author data from multi-source API for: {} with strategy: {}", authorName, strategy);

        try {
            String url = paperSearchBaseUrl + "/api/v1/authors/multi-source/" + authorName;
            if (strategy != null && !strategy.isEmpty()) {
                url += "?strategy=" + strategy;
            }

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new CustomException(
                        "Failed to fetch author data from multi-source API",
                        HttpStatus.SERVICE_UNAVAILABLE,
                        ErrorCode.EXTERNAL_SERVICE_ERROR);
            }

            Map<String, Object> responseBody = response.getBody();
            log.info("Multi-source API response success: {}", responseBody.get("success"));

            if (!(Boolean) responseBody.get("success")) {
                String error = (String) responseBody.get("error");
                throw new CustomException(
                        "Multi-source API returned error: " + error, HttpStatus.NOT_FOUND, ErrorCode.AUTHOR_NOT_FOUND);
            }

            return responseBody;

        } catch (Exception e) {
            log.error("Error fetching author data from multi-source API", e);
            throw new CustomException(
                    "Error communicating with multi-source API: " + e.getMessage(),
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
    }

    /**
     * Update Author entity from multi-source API response
     */
    private void updateAuthorFromMultiSourceResponse(Author author, Map<String, Object> response) {
        try {
            Map<String, Object> authorData = (Map<String, Object>) response.get("author");
            if (authorData == null) {
                throw new CustomException(
                        "No author data in response", HttpStatus.BAD_REQUEST, ErrorCode.DATA_CONVERSION_ERROR);
            }

            log.info("Updating author from multi-source response. Author data keys: {}", authorData.keySet());

            // Core information
            author.setName(getStringValue(authorData, "name"));
            author.setPrimaryAffiliation(getStringValue(authorData, "primary_affiliation"));

            // External identifiers
            author.setSemanticScholarId(getStringValue(authorData, "semantic_scholar_id"));
            author.setOrcidId(getStringValue(authorData, "orcid_id"));
            author.setGoogleScholarId(getStringValue(authorData, "google_scholar_id"));
            author.setOpenalexId(getStringValue(authorData, "openalex_id"));

            // Metrics
            author.setCitationCount(getIntegerValue(authorData, "citation_count"));
            author.setHIndex(getIntegerValue(authorData, "h_index"));
            author.setI10Index(getIntegerValue(authorData, "i10_index"));
            author.setPaperCount(getIntegerValue(authorData, "paper_count"));

            // Publication timeline
            author.setFirstPublicationYear(getIntegerValue(authorData, "first_publication_year"));
            author.setLastPublicationYear(getIntegerValue(authorData, "last_publication_year"));

            // Research information
            author.setAllAffiliations(convertListToJsonString(authorData.get("all_affiliations")));
            author.setResearchAreas(convertListToJsonString(authorData.get("research_areas")));
            author.setRecentPublications(convertObjectToJsonString(authorData.get("recent_publications")));

            // Data source tracking
            author.setDataSources(convertListToJsonString(authorData.get("data_sources")));
            author.setDataQualityScore(getDoubleValue(authorData, "data_quality_score"));
            author.setSearchStrategy((String) response.get("search_strategy"));
            author.setSourcesAttempted(convertListToJsonString(response.get("sources_attempted")));
            author.setSourcesSuccessful(convertListToJsonString(response.get("sources_successful")));

        } catch (Exception e) {
            log.error("Error updating author from multi-source response", e);
            throw new CustomException(
                    "Error processing multi-source author data: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCode.DATA_CONVERSION_ERROR);
        }
    }

    /**
     * Update Author entity from AuthorDto (legacy method for backward
     * compatibility)
     */
    private void updateAuthorFromDto(Author author, AuthorDto dto) {
        // Core information
        author.setName(dto.name());
        author.setPrimaryAffiliation(dto.primaryAffiliation());

        // External identifiers
        author.setSemanticScholarId(dto.semanticScholarId());
        author.setOrcidId(dto.orcidId());
        author.setGoogleScholarId(dto.googleScholarId());
        author.setOpenalexId(dto.openalexId());

        // Metrics
        author.setCitationCount(dto.citationCount());
        author.setHIndex(dto.hIndex());
        author.setI10Index(dto.i10Index());
        author.setPaperCount(dto.paperCount());

        // Publication timeline
        author.setFirstPublicationYear(dto.firstPublicationYear());
        author.setLastPublicationYear(dto.lastPublicationYear());

        // Research information
        author.setAllAffiliations(convertListToJsonString(dto.allAffiliations()));
        author.setResearchAreas(convertListToJsonString(dto.researchAreas()));
        author.setRecentPublications(convertObjectToJsonString(dto.recentPublications()));

        // Data source tracking
        author.setDataSources(convertListToJsonString(dto.dataSources()));
        author.setDataQualityScore(dto.dataQualityScore());
        author.setSearchStrategy(dto.searchStrategy());
        author.setSourcesAttempted(convertListToJsonString(dto.sourcesAttempted()));
        author.setSourcesSuccessful(convertListToJsonString(dto.sourcesSuccessful()));

        // Sync status
        author.setIsSynced(dto.isSynced());
        author.setLastSyncAt(dto.lastSyncAt());
        author.setSyncError(dto.syncError());

        // Legacy fields for compatibility
        author.setHomepageUrl(dto.homepageUrl());
        author.setEmail(dto.email());
        author.setProfileImageUrl(dto.profileImageUrl());
    }

    // Helper methods for extracting values from response map
    private String getStringValue(Map<String, Object> map, String key) {
        return getStringValue(map, key, null);
    }

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private Integer getIntegerValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> convertToStringList(Object obj) {
        if (obj instanceof List) {
            return ((List<?>) obj)
                    .stream().filter(Objects::nonNull).map(Object::toString).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<Object> convertToObjectList(Object obj) {
        if (obj instanceof List) {
            return new ArrayList<>((List<Object>) obj);
        }
        return Collections.emptyList();
    }

    /**
     * Convert list to JSON string
     */
    private String convertListToJsonString(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Error converting list to JSON string", e);
            return null;
        }
    }

    /**
     * Convert object to JSON string
     */
    private String convertObjectToJsonString(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Error converting object to JSON string", e);
            return null;
        }
    }

    private String extractPrimaryAffiliation(Object affiliations) {
        if (affiliations instanceof List) {
            List<?> affList = (List<?>) affiliations;
            if (!affList.isEmpty() && affList.get(0) instanceof Map) {
                Map<?, ?> firstAff = (Map<?, ?>) affList.get(0);
                Object institutionName = firstAff.get("institution_name");
                return institutionName != null ? institutionName.toString() : null;
            }
        }
        return null;
    }
}
