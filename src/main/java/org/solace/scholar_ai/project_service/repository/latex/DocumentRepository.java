package org.solace.scholar_ai.project_service.repository.latex;

import java.util.List;
import java.util.UUID;
import org.solace.scholar_ai.project_service.model.latex.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByProjectIdOrderByUpdatedAtDesc(UUID projectId);
}
