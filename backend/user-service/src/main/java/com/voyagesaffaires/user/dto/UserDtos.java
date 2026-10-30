package com.voyagesaffaires.user.dto;

import com.voyagesaffaires.common.enums.DomaineActivite;
import com.voyagesaffaires.common.enums.StatutInvitation;
import com.voyagesaffaires.user.entity.Entreprise;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** DTOs comptes, entreprises et invitations des employes. */
public final class UserDtos {

    private UserDtos() {
    }

    public record EntrepriseDto(Long id, String nom, DomaineActivite domaineActivite, String pays, String ville,
                                String adresse, String telephone, String siteWeb, String description,
                                long nbEmployes, LocalDateTime dateCreation) {

        public static EntrepriseDto from(Entreprise e, long nbEmployes) {
            return new EntrepriseDto(e.getId(), e.getNom(), e.getDomaineActivite(), e.getPays(), e.getVille(),
                    e.getAdresse(), e.getTelephone(), e.getSiteWeb(), e.getDescription(), nbEmployes,
                    e.getDateCreation());
        }
    }

    public record UpdateProfilRequest(
            @NotBlank String nom,
            @NotBlank String prenom,
            String telephone,
            String poste) {
    }

    public record UpdateEntrepriseRequest(
            @NotBlank String nom,
            @NotNull DomaineActivite domaineActivite,
            @NotBlank String pays,
            @NotBlank String ville,
            String adresse,
            String telephone,
            String siteWeb,
            @Size(max = 2000) String description) {
    }

    public record ChangePasswordRequest(
            @NotBlank String ancienMotDePasse,
            @NotBlank @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caracteres") String nouveauMotDePasse) {
    }

    public record InvitationRequest(
            @NotBlank @Email(message = "Email invalide") String email,
            String poste) {
    }

    public record InvitationDto(Long id, String email, String poste, StatutInvitation statut,
                                LocalDateTime dateCreation, LocalDateTime dateExpiration, String lien) {
    }
}
