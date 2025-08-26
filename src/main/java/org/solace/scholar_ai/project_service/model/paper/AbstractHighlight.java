package org.solace.scholar_ai.project_service.model.paper;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "abstract_highlights")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbstractHighlight {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "abstract_analysis_id", nullable = false)
    private AbstractAnalysis abstractAnalysis;

    @Column(name = "text", nullable = false, length = 500)
    private String text;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "start_index", nullable = false)
    private Integer startIndex;

    @Column(name = "end_index", nullable = false)
    private Integer endIndex;
}
