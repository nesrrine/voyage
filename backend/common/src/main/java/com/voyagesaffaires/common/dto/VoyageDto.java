package com.voyagesaffaires.common.dto;

import com.voyagesaffaires.common.enums.DomaineActivite;
import com.voyagesaffaires.common.enums.ObjetVoyage;
import com.voyagesaffaires.common.enums.StatutTemporel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/** Voyage expose par le voyage-service (et lu par l'ai-service via OpenFeign). */
public record VoyageDto(Long id, String titre, String villeDestination, String paysDestination,
                        double latitude, double longitude, int rayonKm,
                        LocalDate dateDebut, LocalDate dateFin, long dureeJours,
                        ObjetVoyage objet, String description, Set<DomaineActivite> domainesRecherches,
                        boolean visible, Long employeId, String employeNom, String employePoste,
                        Long entrepriseId, String entrepriseNom, DomaineActivite entrepriseDomaine,
                        StatutTemporel statutTemporel, LocalDateTime dateCreation) {
}
