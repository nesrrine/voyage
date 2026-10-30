package com.voyagesaffaires.user.dto;

import com.voyagesaffaires.common.dto.UserDto;
import com.voyagesaffaires.common.enums.DomaineActivite;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** DTOs d'inscription et d'authentification. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {
    }

    public record RegisterEntrepriseRequest(
            @NotBlank(message = "Le nom de l'entreprise est obligatoire") String nomEntreprise,
            @NotNull(message = "Le domaine d'activite est obligatoire") DomaineActivite domaineActivite,
            @NotBlank String pays,
            @NotBlank String ville,
            String adresse,
            String telephone,
            String siteWeb,
            @Size(max = 2000) String description,
            @NotBlank String nom,
            @NotBlank String prenom,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caracteres") String password) {
    }

    public record RegisterEmployeRequest(
            @NotBlank String token,
            @NotBlank String nom,
            @NotBlank String prenom,
            String telephone,
            String poste,
            @NotBlank @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caracteres") String password) {
    }

    public record AuthResponse(String token, UserDto user) {
    }

    public record InvitationInfo(String email, String entrepriseNom, String poste) {
    }
}
