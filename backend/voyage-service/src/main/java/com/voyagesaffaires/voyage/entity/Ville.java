package com.voyagesaffaires.voyage.entity;

import jakarta.persistence.*;
import lombok.*;

/** Referentiel des villes (coordonnees GPS utilisees par l'algorithme de matching). */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ville {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String pays;

    private double latitude;
    private double longitude;
}
