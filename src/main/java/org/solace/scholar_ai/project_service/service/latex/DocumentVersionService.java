package org.solace.scholar_ai.project_service.service.latex;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.latex.DocumentVersionDTO;
import org.solace.scholar_ai.project_service.model.latex.DocumentVersion;
import org.solace.scholar_ai.project_service.repository.latex.DocumentVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentVersionService {

    private final DocumentVersionRepository documentVersionRepository;

    @Transactional
    public DocumentVersionDTO createVersion(UUID documentId, String content, String commitMessage, UUID createdBy) {
        // Get the next version number
        Integer nextVersionNumber = getNextVersionNumber(documentId);

        DocumentVersion version = DocumentVersion.builder()
                .documentId(documentId)
                .versionNumber(nextVersionNumber)
                .content(content)
                .commitMessage(commitMessage)
                .createdBy(createdBy)
                .isAutoSave(false)
                .build();

        DocumentVersion savedVersion = documentVersionRepository.save(version);
        log.info("Created version {} for document {}", nextVersionNumber, documentId);

        return convertToDTO(savedVersion);
    }

    @Transactional
    public DocumentVersionDTO createAutoSaveVersion(UUID documentId, String content) {
        // Get the next version number
        Integer nextVersionNumber = getNextVersionNumber(documentId);

        DocumentVersion version = DocumentVersion.builder()
                .documentId(documentId)
                .versionNumber(nextVersionNumber)
                .content(content)
                .commitMessage("Auto-save")
                .createdBy(null)
                .isAutoSave(true)
                .build();

        DocumentVersion savedVersion = documentVersionRepository.save(version);
        log.info("Created auto-save version {} for document {}", nextVersionNumber, documentId);

        return convertToDTO(savedVersion);
    }

    public List<DocumentVersionDTO> getVersionHistory(UUID documentId) {
        List<DocumentVersion> versions = documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId);
        return versions.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<DocumentVersionDTO> getManualVersions(UUID documentId) {
        List<DocumentVersion> versions =
                documentVersionRepository.findByDocumentIdAndIsAutoSaveFalseOrderByVersionNumberDesc(documentId);
        return versions.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public Optional<DocumentVersionDTO> getVersion(UUID documentId, Integer versionNumber) {
        return documentVersionRepository
                .findByDocumentIdAndVersionNumber(documentId, versionNumber)
                .map(this::convertToDTO);
    }

    public Optional<DocumentVersionDTO> getLatestVersion(UUID documentId) {
        return documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId).stream()
                .findFirst()
                .map(this::convertToDTO);
    }

    public Optional<DocumentVersionDTO> getPreviousVersion(UUID documentId, Integer currentVersion) {
        return documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId).stream()
                .filter(v -> v.getVersionNumber() < currentVersion)
                .findFirst()
                .map(this::convertToDTO);
    }

    public Optional<DocumentVersionDTO> getNextVersion(UUID documentId, Integer currentVersion) {
        return documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId).stream()
                .filter(v -> v.getVersionNumber() > currentVersion)
                .sorted((v1, v2) -> Integer.compare(v1.getVersionNumber(), v2.getVersionNumber()))
                .findFirst()
                .map(this::convertToDTO);
    }

    public long getVersionCount(UUID documentId) {
        return documentVersionRepository.countByDocumentId(documentId);
    }

    private Integer getNextVersionNumber(UUID documentId) {
        Optional<Integer> maxVersion = documentVersionRepository.findMaxVersionNumberByDocumentId(documentId);
        return maxVersion.map(v -> v + 1).orElse(1);
    }

    private DocumentVersionDTO convertToDTO(DocumentVersion version) {
        return DocumentVersionDTO.builder()
                .id(version.getId())
                .documentId(version.getDocumentId())
                .versionNumber(version.getVersionNumber())
                .content(version.getContent())
                .commitMessage(version.getCommitMessage())
                .createdBy(version.getCreatedBy())
                .createdAt(version.getCreatedAt())
                .isAutoSave(version.getIsAutoSave())
                .build();
    }
}
