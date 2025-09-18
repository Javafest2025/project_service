package org.solace.scholar_ai.project_service.service.readinglist;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.readinglist.AddReadingListItemDto;
import org.solace.scholar_ai.project_service.dto.readinglist.ReadingListItemDto;
import org.solace.scholar_ai.project_service.dto.readinglist.UpdateReadingListItemDto;
import org.solace.scholar_ai.project_service.mapping.readinglist.ReadingListItemMapper;
import org.solace.scholar_ai.project_service.model.readinglist.ReadingListItem;
import org.solace.scholar_ai.project_service.repository.project.ProjectRepository;
import org.solace.scholar_ai.project_service.repository.readinglist.ReadingListItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReadingListItemService {

    private final ReadingListItemRepository readingListItemRepository;
    private final ProjectRepository projectRepository;
    private final ReadingListItemMapper readingListItemMapper;

    /**
     * Get all reading list items for a project with filtering and pagination
     */
    public Page<ReadingListItemDto> getReadingListItems(
            UUID projectId, UUID userId, Map<String, String> filters, Pageable pageable) {
        validateProjectAccess(projectId, userId);

        String status = filters.get("status");
        String priority = filters.get("priority");
        String difficulty = filters.get("difficulty");
        String relevance = filters.get("relevance");
        Boolean isBookmarked =
                filters.get("isBookmarked") != null ? Boolean.valueOf(filters.get("isBookmarked")) : null;
        Boolean isRecommended =
                filters.get("isRecommended") != null ? Boolean.valueOf(filters.get("isRecommended")) : null;
        String sortBy = filters.get("sortBy");
        String sortOrder = filters.get("sortOrder");

        Page<ReadingListItem> items = readingListItemRepository.findByProjectIdWithFilters(
                projectId,
                status,
                priority,
                difficulty,
                relevance,
                isBookmarked,
                isRecommended,
                sortBy,
                sortOrder,
                pageable);

        return items.map(readingListItemMapper::toDto);
    }

    /**
     * Add a new paper to the reading list
     */
    public ReadingListItemDto addReadingListItem(UUID projectId, UUID userId, AddReadingListItemDto dto) {
        validateProjectAccess(projectId, userId);

        // Check if paper already exists in reading list
        if (readingListItemRepository.existsByProjectIdAndPaperId(projectId, dto.paperId())) {
            throw new RuntimeException("Paper already exists in reading list");
        }

        ReadingListItem item = readingListItemMapper.fromAddDto(dto, projectId);
        ReadingListItem savedItem = readingListItemRepository.save(item);

        log.info("Added paper {} to reading list for project {}", dto.paperId(), projectId);
        return readingListItemMapper.toDto(savedItem);
    }

    /**
     * Update a reading list item
     */
    public ReadingListItemDto updateReadingListItem(
            UUID projectId, UUID itemId, UUID userId, UpdateReadingListItemDto dto) {
        validateProjectAccess(projectId, userId);

        ReadingListItem item = readingListItemRepository
                .findByIdAndProjectId(itemId, projectId)
                .orElseThrow(() -> new RuntimeException("Reading list item not found"));

        // Handle status changes and timestamps
        handleStatusChange(item, dto.status());

        // Update the item
        ReadingListItem updatedItem = readingListItemMapper.fromUpdateDto(dto, item);
        ReadingListItem savedItem = readingListItemRepository.save(updatedItem);

        log.info("Updated reading list item {} for project {}", itemId, projectId);
        return readingListItemMapper.toDto(savedItem);
    }

    /**
     * Update reading list item status
     */
    public ReadingListItemDto updateStatus(UUID projectId, UUID itemId, UUID userId, String status) {
        validateProjectAccess(projectId, userId);

        ReadingListItem item = readingListItemRepository
                .findByIdAndProjectId(itemId, projectId)
                .orElseThrow(() -> new RuntimeException("Reading list item not found"));

        handleStatusChange(item, status);
        ReadingListItem savedItem = readingListItemRepository.save(item);

        log.info("Updated status of reading list item {} to {} for project {}", itemId, status, projectId);
        return readingListItemMapper.toDto(savedItem);
    }

    /**
     * Update reading progress
     */
    public ReadingListItemDto updateProgress(UUID projectId, UUID itemId, UUID userId, Integer progress) {
        validateProjectAccess(projectId, userId);

        if (progress < 0 || progress > 100) {
            throw new RuntimeException("Progress must be between 0 and 100");
        }

        ReadingListItem item = readingListItemRepository
                .findByIdAndProjectId(itemId, projectId)
                .orElseThrow(() -> new RuntimeException("Reading list item not found"));

        item.setReadingProgress(progress);
        item.setLastReadAt(Instant.now());

        // Auto-update status based on progress
        if (progress == 0 && item.getStatus() == ReadingListItem.Status.IN_PROGRESS) {
            item.setStatus(ReadingListItem.Status.PENDING);
            item.setStartedAt(null);
        } else if (progress > 0 && item.getStatus() == ReadingListItem.Status.PENDING) {
            item.setStatus(ReadingListItem.Status.IN_PROGRESS);
            if (item.getStartedAt() == null) {
                item.setStartedAt(Instant.now());
                item.setReadCount(item.getReadCount() + 1);
            }
        } else if (progress == 100 && item.getStatus() != ReadingListItem.Status.COMPLETED) {
            item.setStatus(ReadingListItem.Status.COMPLETED);
            item.setCompletedAt(Instant.now());
        }

        ReadingListItem savedItem = readingListItemRepository.save(item);

        log.info("Updated progress of reading list item {} to {}% for project {}", itemId, progress, projectId);
        return readingListItemMapper.toDto(savedItem);
    }

    /**
     * Delete a reading list item
     */
    public void deleteReadingListItem(UUID projectId, UUID itemId, UUID userId) {
        validateProjectAccess(projectId, userId);

        ReadingListItem item = readingListItemRepository
                .findByIdAndProjectId(itemId, projectId)
                .orElseThrow(() -> new RuntimeException("Reading list item not found"));

        readingListItemRepository.delete(item);
        log.info("Deleted reading list item {} from project {}", itemId, projectId);
    }

    /**
     * Toggle bookmark status
     */
    public ReadingListItemDto toggleBookmark(UUID projectId, UUID itemId, UUID userId) {
        validateProjectAccess(projectId, userId);

        ReadingListItem item = readingListItemRepository
                .findByIdAndProjectId(itemId, projectId)
                .orElseThrow(() -> new RuntimeException("Reading list item not found"));

        item.setIsBookmarked(!item.getIsBookmarked());
        ReadingListItem savedItem = readingListItemRepository.save(item);

        log.info(
                "Toggled bookmark status of reading list item {} to {} for project {}",
                itemId,
                savedItem.getIsBookmarked(),
                projectId);
        return readingListItemMapper.toDto(savedItem);
    }

    /**
     * Add or update notes
     */
    public ReadingListItemDto updateNotes(UUID projectId, UUID itemId, UUID userId, String notes) {
        validateProjectAccess(projectId, userId);

        ReadingListItem item = readingListItemRepository
                .findByIdAndProjectId(itemId, projectId)
                .orElseThrow(() -> new RuntimeException("Reading list item not found"));

        item.setNotes(notes);
        ReadingListItem savedItem = readingListItemRepository.save(item);

        log.info("Updated notes for reading list item {} in project {}", itemId, projectId);
        return readingListItemMapper.toDto(savedItem);
    }

    /**
     * Rate a completed reading list item
     */
    public ReadingListItemDto rateItem(UUID projectId, UUID itemId, UUID userId, Integer rating) {
        validateProjectAccess(projectId, userId);

        if (rating < 1 || rating > 5) {
            throw new RuntimeException("Rating must be between 1 and 5");
        }

        ReadingListItem item = readingListItemRepository
                .findByIdAndProjectId(itemId, projectId)
                .orElseThrow(() -> new RuntimeException("Reading list item not found"));

        if (item.getStatus() != ReadingListItem.Status.COMPLETED) {
            throw new RuntimeException("Rating only allowed for completed items");
        }

        item.setRating(rating);
        ReadingListItem savedItem = readingListItemRepository.save(item);

        log.info("Rated reading list item {} with {} stars for project {}", itemId, rating, projectId);
        return readingListItemMapper.toDto(savedItem);
    }

    /**
     * Get reading list statistics
     */
    public Map<String, Object> getReadingListStats(UUID projectId, UUID userId) {
        validateProjectAccess(projectId, userId);

        long totalItems = readingListItemRepository.countByProjectId(projectId);
        long pendingItems =
                readingListItemRepository.countByProjectIdAndStatus(projectId, ReadingListItem.Status.PENDING);
        long inProgressItems =
                readingListItemRepository.countByProjectIdAndStatus(projectId, ReadingListItem.Status.IN_PROGRESS);
        long completedItems =
                readingListItemRepository.countByProjectIdAndStatus(projectId, ReadingListItem.Status.COMPLETED);
        long skippedItems =
                readingListItemRepository.countByProjectIdAndStatus(projectId, ReadingListItem.Status.SKIPPED);

        Double averageRating = readingListItemRepository.getAverageRatingByProjectId(projectId);
        Long totalEstimatedTime = readingListItemRepository.getTotalEstimatedTimeByProjectId(projectId);
        Long totalActualTime = readingListItemRepository.getTotalActualTimeByProjectId(projectId);

        double completionRate = totalItems > 0 ? (double) completedItems / totalItems * 100 : 0;
        double averageReadingTime =
                completedItems > 0 && totalActualTime != null ? (double) totalActualTime / completedItems : 0;

        return Map.of(
                "totalItems", totalItems,
                "pendingItems", pendingItems,
                "inProgressItems", inProgressItems,
                "completedItems", completedItems,
                "skippedItems", skippedItems,
                "totalEstimatedTime", totalEstimatedTime != null ? totalEstimatedTime : 0,
                "totalActualTime", totalActualTime != null ? totalActualTime : 0,
                "averageRating", averageRating != null ? averageRating : 0.0,
                "completionRate", completionRate,
                "averageReadingTime", averageReadingTime);
    }

    /**
     * Get recommended items (placeholder for AI recommendations)
     */
    public List<ReadingListItemDto> getRecommendedItems(UUID projectId, UUID userId, int limit) {
        validateProjectAccess(projectId, userId);

        List<ReadingListItem> items = readingListItemRepository.findRecommendedItemsByProjectId(projectId);

        return items.stream().limit(limit).map(readingListItemMapper::toDto).toList();
    }

    /**
     * Validate that user has access to the project
     */
    private void validateProjectAccess(UUID projectId, UUID userId) {
        // Skip validation if userId is null (no authentication required)
        if (userId == null) {
            return;
        }

        // Check if user is the project owner
        projectRepository
                .findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new RuntimeException("Project not found or access denied"));
    }

    /**
     * Handle status changes and update timestamps accordingly
     */
    private void handleStatusChange(ReadingListItem item, String newStatus) {
        if (newStatus == null) return;

        ReadingListItem.Status status = ReadingListItem.Status.valueOf(newStatus.toUpperCase());
        ReadingListItem.Status oldStatus = item.getStatus();

        switch (status) {
            case PENDING:
                if (oldStatus == ReadingListItem.Status.IN_PROGRESS) {
                    item.setStartedAt(null);
                }
                break;
            case IN_PROGRESS:
                if (oldStatus == ReadingListItem.Status.PENDING) {
                    item.setStartedAt(Instant.now());
                    item.setReadCount(item.getReadCount() + 1);
                } else if (oldStatus == ReadingListItem.Status.COMPLETED) {
                    item.setCompletedAt(null);
                }
                break;
            case COMPLETED:
                if (oldStatus != ReadingListItem.Status.COMPLETED) {
                    item.setCompletedAt(Instant.now());
                    if (item.getStartedAt() == null) {
                        item.setStartedAt(Instant.now());
                    }
                }
                break;
            case SKIPPED:
                // No timestamp changes for skipped
                break;
        }

        item.setStatus(status);
        item.setLastReadAt(Instant.now());
    }
}
