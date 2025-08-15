package org.solace.scholar_ai.project_service.model.paper;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "publication_venues")
public class PublicationVenue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "venue_name", length = 300)
    private String venueName;

    @Column(length = 200)
    private String publisher;

    @Column(length = 50)
    private String volume;

    @Column(length = 50)
    private String issue;

    @Column(length = 100)
    private String pages;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paper_id", nullable = false)
    private Paper paper;
}
