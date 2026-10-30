package com.voyagesaffaires.common.dto;

import com.voyagesaffaires.common.enums.DomaineActivite;
import com.voyagesaffaires.common.enums.StatutMatch;

import java.time.LocalDateTime;

/**
 * Invitation de voyage vue par l'utilisateur connecte.
 * Les coordonnees du partenaire ne sont devoilees qu'une fois l'invitation acceptee.
 */
public record MatchDto(Long id, double score, double scoreDomaine, double scoreDate, double scoreDistance,
                       double scoreObjet, double distanceKm, long joursCommuns, StatutMatch statut,
                       LocalDateTime dateCreation, LocalDateTime dateReponse,
                       boolean recue, VoyageDto monVoyage, VoyageDto voyagePartenaire,
                       String partenaireNom, String partenairePoste, String partenaireEntreprise,
                       DomaineActivite partenaireDomaine, String partenaireEmail, String partenaireTelephone) {
}
