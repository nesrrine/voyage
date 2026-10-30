package com.voyagesaffaires.user.entity;

import com.voyagesaffaires.common.enums.DomaineActivite;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Entreprise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DomaineActivite domaineActivite;

    private String pays;
    private String ville;
    private String adresse;
    private String telephone;
    private String siteWeb;

    @Column(length = 2000)
    private String description;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}
