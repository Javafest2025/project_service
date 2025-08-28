package org.solace.scholar_ai.project_service.service.latex;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.latex.CreateDocumentRequestDTO;
import org.solace.scholar_ai.project_service.dto.latex.DocumentResponseDTO;
import org.solace.scholar_ai.project_service.dto.latex.DocumentVersionDTO;
import org.solace.scholar_ai.project_service.dto.latex.UpdateDocumentRequestDTO;
import org.solace.scholar_ai.project_service.mapping.latex.DocumentMapper;
import org.solace.scholar_ai.project_service.model.latex.Document;
import org.solace.scholar_ai.project_service.repository.latex.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentMapper documentMapper;
    private final DocumentVersionService documentVersionService;
    private final LaTeXCompilationService latexCompilationService;
    private final ProfessionalLaTeXService professionalLaTeXService;

    @Transactional
    public DocumentResponseDTO createDocument(CreateDocumentRequestDTO request) {
        Document document = documentMapper.toEntity(request);
        Document savedDocument = documentRepository.save(document);
        return documentMapper.toResponseDTO(savedDocument);
    }

    public List<DocumentResponseDTO> getDocumentsByProjectId(UUID projectId) {
        List<Document> documents = documentRepository.findByProjectIdOrderByUpdatedAtDesc(projectId);
        return documentMapper.toResponseDTOList(documents);
    }

    public DocumentResponseDTO getDocumentById(UUID documentId) {
        Document document = documentRepository
                .findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + documentId));
        return documentMapper.toResponseDTO(document);
    }

    @Transactional
    public DocumentResponseDTO updateDocument(UpdateDocumentRequestDTO request) {
        Document document = documentRepository
                .findById(request.getDocumentId())
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + request.getDocumentId()));

        if (request.getTitle() != null) {
            document.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            // Create a version before updating the document
            documentVersionService.createVersion(
                    document.getId(), document.getContent(), "Content updated", null // createdBy can be null for now
                    );

            document.setContent(request.getContent());
            // Calculate file size based on content length
            document.setFileSize((long) request.getContent().length());
            // Increment version on content change
            document.setVersion(document.getVersion() + 1);
        }

        Document savedDocument = documentRepository.save(document);
        return documentMapper.toResponseDTO(savedDocument);
    }

    @Transactional
    public DocumentResponseDTO autoSaveDocument(UUID documentId, String content) {
        Document document = documentRepository
                .findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + documentId));

        document.setContent(content);
        document.setFileSize((long) content.length());
        document.setIsAutoSaved(true);
        // Update last accessed time
        document.setLastAccessed(java.time.Instant.now());

        Document savedDocument = documentRepository.save(document);
        return documentMapper.toResponseDTO(savedDocument);
    }

    @Transactional
    public void updateLastAccessed(UUID documentId) {
        documentRepository.updateLastAccessed(documentId, java.time.Instant.now());
    }

    public DocumentResponseDTO createDocumentWithName(UUID projectId, String fileName) {
        // Ensure file has .tex extension
        if (!fileName.endsWith(".tex")) {
            fileName = fileName + ".tex";
        }

        // Handle duplicate names by appending a number
        String finalFileName = fileName;
        int counter = 1;

        while (documentRepository
                .findByProjectIdAndTitle(projectId, finalFileName)
                .isPresent()) {
            String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf("."));
            String extension = fileName.substring(fileName.lastIndexOf("."));
            finalFileName = nameWithoutExt + " (" + counter + ")" + extension;
            counter++;
        }

        Document document = Document.builder()
                .projectId(projectId)
                .title(finalFileName)
                .content(
                        "% " + finalFileName
                                + "\n\\documentclass{article}\n\\begin{document}\n\n% Start writing your LaTeX document here...\n\n\\end{document}")
                .documentType(org.solace.scholar_ai.project_service.model.latex.DocumentType.LATEX)
                .fileExtension("tex")
                .fileSize(0L)
                .version(1)
                .isAutoSaved(false)
                .build();

        Document savedDocument = documentRepository.save(document);
        return documentMapper.toResponseDTO(savedDocument);
    }

    public List<DocumentResponseDTO> searchDocuments(UUID projectId, String query) {
        List<Document> documents =
                documentRepository.findByProjectIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(projectId, query);
        return documentMapper.toResponseDTOList(documents);
    }

    public long getDocumentCount(UUID projectId) {
        return documentRepository.countByProjectId(projectId);
    }

    public List<DocumentVersionDTO> getDocumentVersionHistory(UUID documentId) {
        return documentVersionService.getVersionHistory(documentId);
    }

    public DocumentVersionDTO getDocumentVersion(UUID documentId, Integer versionNumber) {
        return documentVersionService
                .getVersion(documentId, versionNumber)
                .orElseThrow(() -> new RuntimeException("Version not found"));
    }

    public DocumentVersionDTO getPreviousVersion(UUID documentId, Integer currentVersion) {
        return documentVersionService
                .getPreviousVersion(documentId, currentVersion)
                .orElseThrow(() -> new RuntimeException("No previous version found"));
    }

    public DocumentVersionDTO getNextVersion(UUID documentId, Integer currentVersion) {
        return documentVersionService
                .getNextVersion(documentId, currentVersion)
                .orElseThrow(() -> new RuntimeException("No next version found"));
    }

    public DocumentVersionDTO createManualVersion(UUID documentId, String content, String commitMessage) {
        return documentVersionService.createVersion(documentId, content, commitMessage, null);
    }

    @Transactional
    public void deleteDocument(UUID documentId) {
        if (!documentRepository.existsById(documentId)) {
            throw new RuntimeException("Document not found with id: " + documentId);
        }
        documentRepository.deleteById(documentId);
    }

    public String compileLatex(String latexContent) {
        try {
            // Use the new professional LaTeX service
            return professionalLaTeXService.compileLatex(latexContent);
        } catch (Exception e) {
            log.error("Professional LaTeX compilation failed", e);
            // Fallback to old service
            try {
                return latexCompilationService.compileLatexToHtml(latexContent);
            } catch (Exception fallbackException) {
                log.error("Fallback compilation also failed", fallbackException);
                return latexCompilationService.compileLatexFallback(latexContent);
            }
        }
    }
}
