package org.solace.scholar_ai.project_service.model.latex;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType = DocumentType.LATEX;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_extension", length = 10)
    private String fileExtension = "tex";

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "last_accessed")
    private Instant lastAccessed;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "is_auto_saved", nullable = false)
    private Boolean isAutoSaved = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
