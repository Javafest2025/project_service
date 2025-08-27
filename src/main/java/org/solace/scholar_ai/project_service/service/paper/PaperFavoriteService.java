package org.solace.scholar_ai.project_service.service.paper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.paper.PaperDto;
import org.solace.scholar_ai.project_service.dto.paper.PaperFavoriteRequest;
import org.solace.scholar_ai.project_service.dto.paper.PaperFavoriteResponse;
import org.solace.scholar_ai.project_service.mapping.paper.PaperMapper;
import org.solace.scholar_ai.project_service.model.paper.Paper;
import org.solace.scholar_ai.project_service.model.paper.ProjectPaperFavorite;
import org.solace.scholar_ai.project_service.model.project.Project;
import org.solace.scholar_ai.project_service.repository.paper.PaperRepository;
import org.solace.scholar_ai.project_service.repository.paper.ProjectPaperFavoriteRepository;
import org.solace.scholar_ai.project_service.repository.project.ProjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaperFavoriteService {

    private final ProjectPaperFavoriteRepository favoriteRepository;
    private final PaperRepository paperRepository;
    private final ProjectRepository projectRepository;
    private final PaperMapper paperMapper;

    @Transactional
    public PaperFavoriteResponse addToFavorites(
            UUID projectId, UUID paperId, UUID userId, PaperFavoriteRequest request) {
        log.info("Adding paper {} to favorites for project {} by user {}", paperId, projectId, userId);

        // Validate project exists
        Project project =
                projectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found"));

        // Validate paper exists
        Paper paper = paperRepository.findById(paperId).orElseThrow(() -> new RuntimeException("Paper not found"));

        // Check if already favorited
        Optional<ProjectPaperFavorite> existingFavorite =
                favoriteRepository.findByProjectIdAndPaperIdAndUserId(projectId, paperId, userId);

        if (existingFavorite.isPresent()) {
            log.warn("Paper {} is already favorited in project {} by user {}", paperId, projectId, userId);
            throw new RuntimeException("Paper is already favorited");
        }

        // Create new favorite
        ProjectPaperFavorite favorite = ProjectPaperFavorite.builder()
                .project(project)
                .paper(paper)
                .userId(userId)
                .notes(request.getNotes())
                .priority(request.getPriority())
                .tags(request.getTags())
                .build();

        ProjectPaperFavorite savedFavorite = favoriteRepository.save(favorite);
        return mapToResponse(savedFavorite);
    }

    @Transactional
    public void removeFromFavorites(UUID projectId, UUID paperId, UUID userId) {
        log.info("Removing paper {} from favorites for project {} by user {}", paperId, projectId, userId);

        Optional<ProjectPaperFavorite> favorite =
                favoriteRepository.findByProjectIdAndPaperIdAndUserId(projectId, paperId, userId);

        if (favorite.isEmpty()) {
            log.warn("Paper {} is not favorited in project {} by user {}", paperId, projectId, userId);
            throw new RuntimeException("Paper is not favorited");
        }

        favoriteRepository.delete(favorite.get());
    }

    @Transactional
    public PaperFavoriteResponse toggleFavorite(
            UUID projectId, UUID paperId, UUID userId, PaperFavoriteRequest request) {
        log.info("Toggling favorite status for paper {} in project {} by user {}", paperId, projectId, userId);

        Optional<ProjectPaperFavorite> existingFavorite =
                favoriteRepository.findByProjectIdAndPaperIdAndUserId(projectId, paperId, userId);

        if (existingFavorite.isPresent()) {
            // Remove from favorites
            favoriteRepository.delete(existingFavorite.get());
            return null; // Return null to indicate removed
        } else {
            // Add to favorites
            return addToFavorites(projectId, paperId, userId, request);
        }
    }

    @Transactional(readOnly = true)
    public boolean isFavorited(UUID projectId, UUID paperId, UUID userId) {
        return favoriteRepository.existsByProjectIdAndPaperIdAndUserId(projectId, paperId, userId);
    }

    @Transactional(readOnly = true)
    public List<PaperFavoriteResponse> getFavorites(UUID projectId, UUID userId) {
        log.info("Getting favorites for project {} by user {}", projectId, userId);

        List<ProjectPaperFavorite> favorites = favoriteRepository.findByProjectIdAndUserId(projectId, userId);
        return favorites.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PaperFavoriteResponse> getFavoritesPaginated(UUID projectId, UUID userId, Pageable pageable) {
        log.info("Getting paginated favorites for project {} by user {}", projectId, userId);

        Page<ProjectPaperFavorite> favorites = favoriteRepository.findByProjectIdAndUserId(projectId, userId, pageable);
        return favorites.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public long getFavoriteCount(UUID projectId, UUID userId) {
        return favoriteRepository.countByProjectIdAndUserId(projectId, userId);
    }

    @Transactional
    public PaperFavoriteResponse updateFavorite(
            UUID projectId, UUID paperId, UUID userId, PaperFavoriteRequest request) {
        log.info("Updating favorite for paper {} in project {} by user {}", paperId, projectId, userId);

        ProjectPaperFavorite favorite = favoriteRepository
                .findByProjectIdAndPaperIdAndUserId(projectId, paperId, userId)
                .orElseThrow(() -> new RuntimeException("Favorite not found"));

        favorite.setNotes(request.getNotes());
        favorite.setPriority(request.getPriority());
        favorite.setTags(request.getTags());

        ProjectPaperFavorite updatedFavorite = favoriteRepository.save(favorite);
        return mapToResponse(updatedFavorite);
    }

    private PaperFavoriteResponse mapToResponse(ProjectPaperFavorite favorite) {
        PaperDto paperDto = paperMapper.toDto(favorite.getPaper());

        return PaperFavoriteResponse.builder()
                .id(favorite.getId())
                .projectId(favorite.getProject().getId())
                .paperId(favorite.getPaper().getId())
                .userId(favorite.getUserId())
                .notes(favorite.getNotes())
                .priority(favorite.getPriority())
                .tags(favorite.getTags())
                .createdAt(favorite.getCreatedAt())
                .updatedAt(favorite.getUpdatedAt())
                .paper(paperDto)
                .build();
    }
}
