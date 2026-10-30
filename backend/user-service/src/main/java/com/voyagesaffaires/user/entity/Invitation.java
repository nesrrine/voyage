package com.voyagesaffaires.user.entity;

import com.voyagesaffaires.common.enums.StatutInvitation;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Invitation envoyee par une entreprise a un employe pour rejoindre l'application. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    private String poste;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Entreprise entreprise;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatutInvitation statut = StatutInvitation.EN_ATTENTE;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    private LocalDateTime dateExpiration;

    public boolean isExpiree() {
        return dateExpiration != null && dateExpiration.isBefore(LocalDateTime.now());
    }
}
