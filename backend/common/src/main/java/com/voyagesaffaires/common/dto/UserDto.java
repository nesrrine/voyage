package com.voyagesaffaires.common.dto;

import com.voyagesaffaires.common.enums.DomaineActivite;
import com.voyagesaffaires.common.enums.Role;

import java.time.LocalDateTime;

/** Utilisateur expose par le user-service (et lu par le voyage-service via OpenFeign). */
public record UserDto(Long id, String nom, String prenom, String email, String telephone, String poste,
                      Role role, boolean actif, Long entrepriseId, String entrepriseNom,
                      DomaineActivite domaineActivite, LocalDateTime dateCreation) {

    public String nomComplet() {
        return ((prenom == null ? "" : prenom + " ") + (nom == null ? "" : nom)).trim();
    }
}
