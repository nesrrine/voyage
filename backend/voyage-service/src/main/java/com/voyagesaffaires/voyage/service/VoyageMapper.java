package com.voyagesaffaires.voyage.service;

import com.voyagesaffaires.common.dto.VoyageDto;
import com.voyagesaffaires.common.enums.StatutTemporel;
import com.voyagesaffaires.voyage.entity.Voyage;

import java.time.LocalDate;
import java.util.Set;

public final class VoyageMapper {

    private VoyageMapper() {
    }

    public static VoyageDto toDto(Voyage v) {
        LocalDate today = LocalDate.now();
        StatutTemporel st = v.getDateFin().isBefore(today) ? StatutTemporel.TERMINE
                : v.getDateDebut().isAfter(today) ? StatutTemporel.A_VENIR : StatutTemporel.EN_COURS;
        return new VoyageDto(v.getId(), v.getTitre(), v.getVilleDestination(), v.getPaysDestination(),
                v.getLatitude(), v.getLongitude(), v.getRayonKm(),
                v.getDateDebut(), v.getDateFin(), v.getDureeJours(),
                v.getObjet(), v.getDescription(), Set.copyOf(v.getDomainesRecherches()),
                v.isVisible(), v.getEmployeId(), v.getEmployeNom(), v.getEmployePoste(),
                v.getEntrepriseId(), v.getEntrepriseNom(), v.getEntrepriseDomaine(),
                st, v.getDateCreation());
    }
}
