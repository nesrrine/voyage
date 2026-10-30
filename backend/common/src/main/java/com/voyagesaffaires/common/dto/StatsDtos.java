package com.voyagesaffaires.common.dto;

import java.util.List;
import java.util.Map;

/** Statistiques : tableau de bord (voyage-service) et indicateurs fournis par les autres services. */
public final class StatsDtos {

    private StatsDtos() {
    }

    public record CarteStat(String label, Number valeur, String icone) {
    }

    public record Serie(List<String> labels, List<Number> data) {

        public static Serie vide() {
            return new Serie(List.of(), List.of());
        }
    }

    public record DashboardDto(List<CarteStat> cartes, Map<String, Serie> graphiques) {
    }

    /** Indicateurs du user-service (selon le role de l'appelant). */
    public record UserStats(long entreprises, long employes, long invitationsEnAttente, Serie entreprisesParDomaine) {

        public static UserStats vide() {
            return new UserStats(0, 0, 0, Serie.vide());
        }
    }

    /** Indicateurs du forum-service. */
    public record ForumStats(long publications, Serie parCategorie) {

        public static ForumStats vide() {
            return new ForumStats(0, Serie.vide());
        }
    }
}
