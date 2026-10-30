package com.voyagesaffaires.voyage.entity;

import com.voyagesaffaires.common.enums.DomaineActivite;
import com.voyagesaffaires.common.enums.ObjetVoyage;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

/**
 * Voyage d'affaires. Chaque microservice a sa propre base : l'employe et son entreprise
 * (geres par le user-service) sont references par leur identifiant, avec une copie des
 * informations utiles au matching et a l'affichage.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voyage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    // ----- Employe (reference vers le user-service) -----
    @Column(nullable = false)
    private Long employeId;
    private String employeNom;
    private String employePoste;
    private String employeEmail;
    private String employeTelephone;

    @Builder.Default
    private boolean employeActif = true;

    // ----- Entreprise de l'employe -----
    @Column(nullable = false)
    private Long entrepriseId;
    private String entrepriseNom;

    @Enumerated(EnumType.STRING)
    private DomaineActivite entrepriseDomaine;

    // ----- Destination -----
    @Column(nullable = false)
    private String villeDestination;

    @Column(nullable = false)
    private String paysDestination;

    private double latitude;
    private double longitude;

    /** Perimetre (km) autour de la destination dans lequel on accepte un partenaire. */
    @Builder.Default
    private int rayonKm = 50;

    @Column(nullable = false)
    private LocalDate dateDebut;

    @Column(nullable = false)
    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ObjetVoyage objet;

    @Column(length = 2000)
    private String description;

    /** Domaines d'activite des entreprises partenaires recherchees. */
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "voyage_domaines", joinColumns = @JoinColumn(name = "voyage_id"))
    @Column(name = "domaine")
    @Builder.Default
    private Set<DomaineActivite> domainesRecherches = new HashSet<>();

    /** Visible dans les programmes publics et pris en compte par le matching. */
    @Builder.Default
    private boolean visible = true;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    public long getDureeJours() {
        return ChronoUnit.DAYS.between(dateDebut, dateFin) + 1;
    }
}
