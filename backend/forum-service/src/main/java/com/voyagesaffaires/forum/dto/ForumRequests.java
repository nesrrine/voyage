package com.voyagesaffaires.forum.dto;

import com.voyagesaffaires.common.enums.TypeReaction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Requetes du forum (les reponses PostDto / CommentaireDto sont dans le module common). */
public final class ForumRequests {

    private ForumRequests() {
    }

    public record PostRequest(
            @NotBlank(message = "Le titre est obligatoire") @Size(max = 200) String titre,
            @NotBlank(message = "Le contenu est obligatoire") @Size(max = 5000) String contenu,
            String categorie) {
    }

    public record CommentaireRequest(
            @NotBlank(message = "Le commentaire est vide") @Size(max = 2000) String contenu) {
    }

    public record ReactionRequest(@NotNull TypeReaction type) {
    }
}
