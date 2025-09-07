package org.solace.scholar_ai.project_service.service.paper;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.paper.CreatePaperDto;
import org.solace.scholar_ai.project_service.dto.paper.PaperDto;
import org.solace.scholar_ai.project_service.dto.paper.UpdatePaperDto;
import org.solace.scholar_ai.project_service.mapping.paper.PaperMapper;
import org.solace.scholar_ai.project_service.model.paper.Paper;
import org.solace.scholar_ai.project_service.repository.paper.PaperRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaperService {

    private final PaperRepository paperRepository;
    private final PaperMapper paperMapper;

    public List<PaperDto> getAllPapers() {
        log.info("Fetching all papers");
        List<Paper> papers = paperRepository.findAll();
        return paperMapper.toDtoList(papers);
    }

    public Optional<PaperDto> getPaperById(UUID id) {
        log.info("Fetching paper with ID: {}", id);
        return paperRepository.findById(id).map(paperMapper::toDto);
    }

    public PaperDto createPaper(CreatePaperDto createPaperDto) {
        log.info("Creating new paper: {}", createPaperDto.title());
        Paper paper = paperMapper.fromCreateDto(createPaperDto);
        Paper savedPaper = paperRepository.save(paper);
        log.info("Paper created successfully with ID: {}", savedPaper.getId());
        return paperMapper.toDto(savedPaper);
    }

    public PaperDto updatePaper(UUID id, UpdatePaperDto updatePaperDto) {
        log.info("Updating paper with ID: {}", id);

        Paper existingPaper =
                paperRepository.findById(id).orElseThrow(() -> new RuntimeException("Paper not found with id: " + id));

        // Update basic fields
        if (updatePaperDto.title() != null) {
            existingPaper.setTitle(updatePaperDto.title());
        }
        if (updatePaperDto.abstractText() != null) {
            existingPaper.setAbstractText(updatePaperDto.abstractText());
        }
        if (updatePaperDto.publicationDate() != null) {
            existingPaper.setPublicationDate(updatePaperDto.publicationDate());
        }
        if (updatePaperDto.doi() != null) {
            existingPaper.setDoi(updatePaperDto.doi());
        }
        if (updatePaperDto.semanticScholarId() != null) {
            existingPaper.setSemanticScholarId(updatePaperDto.semanticScholarId());
        }
        if (updatePaperDto.source() != null) {
            existingPaper.setSource(updatePaperDto.source());
        }
        if (updatePaperDto.pdfContentUrl() != null) {
            existingPaper.setPdfContentUrl(updatePaperDto.pdfContentUrl());
        }
        if (updatePaperDto.pdfUrl() != null) {
            existingPaper.setPdfUrl(updatePaperDto.pdfUrl());
        }
        if (updatePaperDto.isOpenAccess() != null) {
            existingPaper.setIsOpenAccess(updatePaperDto.isOpenAccess());
        }
        if (updatePaperDto.paperUrl() != null) {
            existingPaper.setPaperUrl(updatePaperDto.paperUrl());
        }
        if (updatePaperDto.publicationTypes() != null) {
            existingPaper.setPublicationTypes(paperMapper.listToString(updatePaperDto.publicationTypes()));
        }
        if (updatePaperDto.fieldsOfStudy() != null) {
            existingPaper.setFieldsOfStudy(paperMapper.listToString(updatePaperDto.fieldsOfStudy()));
        }

        // Note: Authors and external IDs would need more complex handling
        // through the PaperAuthor and ExternalId repositories
        // For now, we'll skip those updates to avoid complexity

        Paper savedPaper = paperRepository.save(existingPaper);
        log.info("Paper updated successfully with ID: {}", savedPaper.getId());

        return paperMapper.toDto(savedPaper);
    }

    public void deletePaper(UUID id) {
        paperRepository.deleteById(id);
    }

    public List<PaperDto> searchPapersByTitle(String title) {
        log.info("Searching papers by title: {}", title);
        List<Paper> papers = paperRepository.findByTitleContainingIgnoreCase(title);
        return paperMapper.toDtoList(papers);
    }

    public List<PaperDto> searchPapersByAuthor(String author) {
        log.info("Searching papers by author: {}", author);
        List<Paper> papers = paperRepository.findByAuthorNameContainingIgnoreCase(author);
        return paperMapper.toDtoList(papers);
    }

    public List<PaperDto> searchPapersByKeyword(String keyword) {
        log.info("Searching papers by keyword: {}", keyword);
        List<Paper> papers = paperRepository.searchByKeyword(keyword);
        return paperMapper.toDtoList(papers);
    }

    public List<PaperDto> getPapersByDateRange(LocalDate startDate, LocalDate endDate) {
        log.info("Fetching papers between {} and {}", startDate, endDate);
        List<Paper> papers = paperRepository.findByPublicationDateBetween(startDate, endDate);
        return paperMapper.toDtoList(papers);
    }

    public List<PaperDto> getPapersByVenue(String venue) {
        log.info("Fetching papers by venue: {}", venue);
        List<Paper> papers = paperRepository.findByVenueNameContainingIgnoreCase(venue);
        return paperMapper.toDtoList(papers);
    }

    public Map<String, Object> getStructuredFacts(UUID paperId) {
        log.info("📄 Getting structured facts for paper: {}", paperId);

        Optional<Paper> paperOptional = paperRepository.findById(paperId);

        if (paperOptional.isEmpty()) {
            log.warn("❌ Paper not found: {}", paperId);
            throw new RuntimeException("Paper not found");
        }

        Paper paper = paperOptional.get();

        // Extract authors
        List<String> authors = paper.getPaperAuthors().stream()
                .map(paperAuthor -> {
                    if (paperAuthor.getAuthor() != null) {
                        return paperAuthor.getAuthor().getName();
                    } else {
                        // If no author entity is available, return a placeholder
                        return "Unknown Author";
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Create response structure
        Map<String, Object> data = new HashMap<>();
        data.put("title", paper.getTitle());
        data.put("authors", authors);
        data.put("abstract", paper.getAbstractText());
        data.put("doi", paper.getDoi());
        data.put("publicationDate", paper.getPublicationDate());
        data.put("source", paper.getSource());

        Map<String, Object> response = new HashMap<>();
        response.put("data", data);
        response.put("status", "success");

        log.info("✅ Retrieved structured facts for paper '{}' with {} authors", paper.getTitle(), authors.size());

        return response;
    }
}
