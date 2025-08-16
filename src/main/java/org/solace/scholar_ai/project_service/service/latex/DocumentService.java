package org.solace.scholar_ai.project_service.service.latex;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.solace.scholar_ai.project_service.dto.latex.CreateDocumentRequestDTO;
import org.solace.scholar_ai.project_service.dto.latex.DocumentResponseDTO;
import org.solace.scholar_ai.project_service.dto.latex.UpdateDocumentRequestDTO;
import org.solace.scholar_ai.project_service.mapping.latex.DocumentMapper;
import org.solace.scholar_ai.project_service.model.latex.Document;
import org.solace.scholar_ai.project_service.repository.latex.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentMapper documentMapper;
    private final LaTeXCompilationService latexCompilationService;

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

    @Transactional
    public void deleteDocument(UUID documentId) {
        if (!documentRepository.existsById(documentId)) {
            throw new RuntimeException("Document not found with id: " + documentId);
        }
        documentRepository.deleteById(documentId);
    }

    public String compileLatex(String latexContent) {
        try {
            // Try using pandoc first
            return latexCompilationService.compileLatexToHtml(latexContent);
        } catch (Exception e) {
            // Fallback to manual conversion if pandoc fails
            return latexCompilationService.compileLatexFallback(latexContent);
        }
    }
}
