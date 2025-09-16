package org.solace.scholar_ai.project_service.service.note;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.note.PaperSuggestionDto;
import org.solace.scholar_ai.project_service.dto.paper.PaperMetadataDto;
import org.solace.scholar_ai.project_service.mapping.note.PaperMentionMapper;
import org.solace.scholar_ai.project_service.model.note.PaperMention;
import org.solace.scholar_ai.project_service.repository.note.PaperMentionRepository;
import org.solace.scholar_ai.project_service.service.paper.PaperPersistenceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "transactionManager")
public class PaperMentionService {

    private final PaperMentionRepository paperMentionRepository;
    private final PaperPersistenceService paperPersistenceService;
    private final PaperMentionMapper paperMentionMapper;

    // Pattern to match @ mentions in note content
    private static final Pattern MENTION_PATTERN = Pattern.compile("@\\[([^\\]]+)\\]\\(([^)]+)\\)");

    /**
     * Search papers for @ mention suggestions
     */
    @Transactional(readOnly = true, transactionManager = "transactionManager")
    public List<PaperSuggestionDto> searchPapersForMention(UUID projectId, String query) {
        log.info("Searching papers for mention in project {} with query: {}", projectId, query);

        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        try {
            // Get all papers for the project
            List<PaperMetadataDto> papers = paperPersistenceService.findPaperDtosByProjectId(projectId);

            // Filter papers based on query (title, authors, abstract)
            String lowerQuery = query.toLowerCase().trim();
            List<PaperMetadataDto> matchingPapers = papers.stream()
                    .filter(paper -> matchesQuery(paper, lowerQuery))
                    .limit(10) // Limit to 10 suggestions
                    .toList();

            // Convert to suggestion DTOs
            return matchingPapers.stream()
                    .map(paperMentionMapper::toSuggestionDto)
                    .toList();

        } catch (Exception e) {
            log.error("Error searching papers for mention in project {}: {}", projectId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Extract and save paper mentions from note content
     */
    public void extractAndSaveMentions(UUID projectId, UUID noteId, String content) {
        log.info("Extracting paper mentions from note {} in project {}", noteId, projectId);

        // Delete existing mentions for this note
        paperMentionRepository.deleteByNoteId(noteId);

        if (content == null || content.trim().isEmpty()) {
            return;
        }

        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            String mentionText = matcher.group(1); // The display text
            String paperIdStr = matcher.group(2); // The paper ID in parentheses

            try {
                UUID paperId = UUID.fromString(paperIdStr);

                // Create and save the mention
                PaperMention mention = new PaperMention();
                mention.setProjectId(projectId);
                mention.setNoteId(noteId);
                mention.setPaperId(paperId);
                mention.setMentionText(mentionText);
                mention.setStartPosition(matcher.start());
                mention.setEndPosition(matcher.end());

                paperMentionRepository.save(mention);

                log.debug("Saved paper mention: {} -> {}", mentionText, paperId);

            } catch (IllegalArgumentException e) {
                log.warn("Invalid paper ID in mention: {}", paperIdStr);
            }
        }

        log.info(
                "Extracted {} paper mentions from note {}",
                paperMentionRepository.findByNoteIdOrderByStartPosition(noteId).size(),
                noteId);
    }

    /**
     * Get all paper mentions for a note
     */
    @Transactional(readOnly = true, transactionManager = "transactionManager")
    public List<PaperMention> getMentionsByNoteId(UUID noteId) {
        return paperMentionRepository.findByNoteIdOrderByStartPosition(noteId);
    }

    /**
     * Get all paper mentions for a project
     */
    @Transactional(readOnly = true, transactionManager = "transactionManager")
    public List<PaperMention> getMentionsByProjectId(UUID projectId) {
        return paperMentionRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    /**
     * Delete all mentions for a note
     */
    public void deleteMentionsByNoteId(UUID noteId) {
        paperMentionRepository.deleteByNoteId(noteId);
        log.info("Deleted all paper mentions for note {}", noteId);
    }

    /**
     * Delete all mentions for a project
     */
    public void deleteMentionsByProjectId(UUID projectId) {
        paperMentionRepository.deleteByProjectId(projectId);
        log.info("Deleted all paper mentions for project {}", projectId);
    }

    /**
     * Check if a paper matches the search query
     */
    private boolean matchesQuery(PaperMetadataDto paper, String query) {
        if (paper.title() != null && paper.title().toLowerCase().contains(query)) {
            return true;
        }

        if (paper.abstractText() != null && paper.abstractText().toLowerCase().contains(query)) {
            return true;
        }

        if (paper.authors() != null) {
            return paper.authors().stream()
                    .anyMatch(author -> author.name().toLowerCase().contains(query));
        }

        return false;
    }
}
