package com.voyagesaffaires.common.dto;

import com.voyagesaffaires.common.enums.TypeReaction;

import java.time.LocalDateTime;
import java.util.List;

/** Publications du forum exposees par le forum-service (et lues par l'ai-service). */
public final class ForumDtos {

    private ForumDtos() {
    }

    public record CommentaireDto(Long id, String contenu, Long auteurId, String auteurNom, String auteurEntreprise,
                                 LocalDateTime dateCreation) {
    }

    public record PostDto(Long id, String titre, String contenu, String categorie,
                          Long auteurId, String auteurNom, String auteurEntreprise, LocalDateTime dateCreation,
                          long likes, long dislikes, TypeReaction maReaction, int nbCommentaires,
                          List<CommentaireDto> commentaires) {
    }
}
