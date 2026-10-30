package com.voyagesaffaires.voyage.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Active le cache Spring (implementation Caffeine, configuree dans application.yml).
 * - "villes"     : referentiel quasi statique, demande a chaque ouverture du formulaire de voyage ;
 * - "programmes" : liste des voyages publics, la page la plus consultee, invalidee a chaque modification.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String VILLES = "villes";
    public static final String PROGRAMMES = "programmes";
}
