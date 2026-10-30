package com.voyagesaffaires.forum.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Commentaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String contenu;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Post post;

    @Column(nullable = false)
    private Long auteurId;
    private String auteurNom;
    private String auteurEntreprise;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}
