package com.voyagesaffaires.voyage.dto;

import com.voyagesaffaires.common.dto.VoyageDto;
import com.voyagesaffaires.common.enums.DomaineActivite;
import com.voyagesaffaires.common.enums.ObjetVoyage;
import com.voyagesaffaires.voyage.entity.Ville;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.Set;

/** DTOs propres au voyage-service (les DTO partages sont dans le module common). */
public final class VoyageDtos {

    private VoyageDtos() {
    }

    public record VoyageRequest(
            @NotBlank(message = "Le titre est obligatoire") String titre,
            @NotBlank(message = "La ville de destination est obligatoire") String villeDestination,
            @NotBlank(message = "Le pays de destination est obligatoire") String paysDestination,
            @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
            @Min(value = 1, message = "Le perimetre doit etre d'au moins 1 km")
            @Max(value = 1000, message = "Le perimetre ne peut pas depasser 1000 km") Integer rayonKm,
            @NotNull(message = "La date de debut est obligatoire") LocalDate dateDebut,
            @NotNull(message = "La date de fin est obligatoire") LocalDate dateFin,
            @NotNull(message = "L'objet du voyage est obligatoire") ObjetVoyage objet,
            @Size(max = 2000) String description,
            Set<DomaineActivite> domainesRecherches,
            Boolean visible,
            /* Renseigne uniquement lorsqu'une entreprise saisit le voyage d'un de ses employes. */
            Long employeId) {
    }

    /** Resultat d'un ajout / modification : le voyage et le nombre de partenaires trouves par le matching. */
    public record VoyageResultat(VoyageDto voyage, int nouveauxMatchs) {
    }

    public record VilleDto(Long id, String nom, String pays, double latitude, double longitude) {

        public static VilleDto from(Ville v) {
            return new VilleDto(v.getId(), v.getNom(), v.getPays(), v.getLatitude(), v.getLongitude());
        }
    }
}
