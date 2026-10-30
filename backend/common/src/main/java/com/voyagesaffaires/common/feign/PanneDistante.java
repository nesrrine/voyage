package com.voyagesaffaires.common.feign;

import java.util.function.Predicate;

/**
 * Indique au circuit breaker Resilience4j quelles erreurs comptent comme des pannes :
 * un refus metier (403, 404...) ne doit pas "ouvrir le circuit" vers un service qui fonctionne.
 * Configure par : resilience4j.circuitbreaker.configs.default.record-failure-predicate
 */
public class PanneDistante implements Predicate<Throwable> {

    @Override
    public boolean test(Throwable erreur) {
        return FeignFallbacks.estPanne(erreur);
    }
}
