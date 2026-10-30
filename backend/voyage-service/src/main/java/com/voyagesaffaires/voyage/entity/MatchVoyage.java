package com.voyagesaffaires.voyage.entity;

import com.voyagesaffaires.common.enums.StatutMatch;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Invitation de voyage generee par l'algorithme de matching :
 * le proprietaire de voyageSource invite le proprietaire de voyageCible.
 */
@Entity
@Table(name = "matchs", uniqueConstraints = @UniqueConstraint(columnNames = {"voyage_source_id", "voyage_cible_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchVoyage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voyage_source_id")
    private Voyage voyageSource;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voyage_cible_id")
    private Voyage voyageCible;

    private double score;
    private double scoreDomaine;
    private double scoreDate;
    private double scoreDistance;
    private double scoreObjet;
    private double distanceKm;
    private long joursCommuns;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatutMatch statut = StatutMatch.EN_ATTENTE;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    private LocalDateTime dateReponse;
}
