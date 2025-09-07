package org.solace.scholar_ai.project_service.controller.summary;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.solace.scholar_ai.project_service.model.paper.Paper;
import org.solace.scholar_ai.project_service.repository.paper.PaperRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class PaperSummaryControllerTest {

    @Mock
    private PaperRepository paperRepository;

    @InjectMocks
    private PaperSummaryController controller;

    @Test
    void getSummarizationStatus_WhenPaperExists_ReturnsStatus() {
        // Arrange
        UUID paperId = UUID.randomUUID();
        Paper paper = Paper.builder()
                .id(paperId)
                .isSummarized(true)
                .summarizationStatus("COMPLETED")
                .summarizationStartedAt(Instant.now().minusSeconds(60))
                .summarizationCompletedAt(Instant.now())
                .summarizationError(null)
                .build();

        when(paperRepository.findById(paperId)).thenReturn(Optional.of(paper));

        // Act
        ResponseEntity<Map<String, Object>> response = controller.getSummarizationStatus(paperId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<String, Object> status = response.getBody();
        assertEquals(paperId, status.get("paperId"));
        assertEquals(true, status.get("isSummarized"));
        assertEquals("COMPLETED", status.get("summarizationStatus"));
        assertNotNull(status.get("summarizationStartedAt"));
        assertNotNull(status.get("summarizationCompletedAt"));
        assertNull(status.get("summarizationError"));
    }

    @Test
    void getSummarizationStatus_WhenPaperNotFound_ReturnsNotFound() {
        // Arrange
        UUID paperId = UUID.randomUUID();
        when(paperRepository.findById(paperId)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Map<String, Object>> response = controller.getSummarizationStatus(paperId);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("error"));
    }

    @Test
    void getSummarizationStatus_WhenPaperNotSummarized_ReturnsCorrectStatus() {
        // Arrange
        UUID paperId = UUID.randomUUID();
        Paper paper = Paper.builder()
                .id(paperId)
                .isSummarized(false)
                .summarizationStatus("PENDING")
                .summarizationStartedAt(null)
                .summarizationCompletedAt(null)
                .summarizationError(null)
                .build();

        when(paperRepository.findById(paperId)).thenReturn(Optional.of(paper));

        // Act
        ResponseEntity<Map<String, Object>> response = controller.getSummarizationStatus(paperId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<String, Object> status = response.getBody();
        assertEquals(false, status.get("isSummarized"));
        assertEquals("PENDING", status.get("summarizationStatus"));
        assertNull(status.get("summarizationStartedAt"));
        assertNull(status.get("summarizationCompletedAt"));
    }
}
