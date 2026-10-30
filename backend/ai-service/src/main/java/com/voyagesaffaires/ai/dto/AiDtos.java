package com.voyagesaffaires.ai.dto;

import com.voyagesaffaires.common.enums.DomaineActivite;
import com.voyagesaffaires.common.enums.ObjetVoyage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** DTOs des fonctionnalites d'intelligence artificielle. */
public final class AiDtos {

    private AiDtos() {
    }

    public record ChatMessage(
            @NotBlank @Pattern(regexp = "user|assistant") String role,
            @NotBlank @Size(max = 4000) String content) {
    }

    public record ChatRequest(@NotEmpty @Size(max = 30) List<@Valid ChatMessage> messages) {
    }

    public record TexteResponse(String texte) {
    }

    public record StatutIa(boolean configure) {
    }

    public record ExplicationMatch(String explication, String messageContact) {
    }

    public record ProgrammeRequest(
            @NotBlank String titre,
            @NotBlank String villeDestination,
            String paysDestination,
            @NotNull LocalDate dateDebut,
            @NotNull LocalDate dateFin,
            @NotNull ObjetVoyage objet,
            Set<DomaineActivite> domainesRecherches) {
    }

    public record ModerationRequest(
            @NotBlank @Size(max = 200) String titre,
            @NotBlank @Size(max = 5000) String contenu) {
    }

    public record ModerationResponse(boolean acceptable, String raison) {
    }
}
