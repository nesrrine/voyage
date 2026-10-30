package com.voyagesaffaires.gateway.security;

import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;

import java.util.Set;

/**
 * Regle d'acces appliquee par la gateway : un chemin (motif Ant), une methode HTTP optionnelle
 * et les roles autorises. Une liste de roles vide signifie "acces public".
 */
public record RegleAcces(String motif, HttpMethod methode, Set<String> roles) {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    public static RegleAcces publique(HttpMethod methode, String motif) {
        return new RegleAcces(motif, methode, Set.of());
    }

    public static RegleAcces roles(String motif, String... roles) {
        return new RegleAcces(motif, null, Set.of(roles));
    }

    public boolean correspond(HttpMethod methodeRequete, String chemin) {
        return (methode == null || methode.equals(methodeRequete)) && MATCHER.match(motif, chemin);
    }

    public boolean estPublique() {
        return roles.isEmpty();
    }
}
