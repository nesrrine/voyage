package com.voyagesaffaires.user.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * Si le voyage-service est indisponible, l'activation / desactivation de l'employe reussit quand meme :
 * seule la synchronisation de ses voyages est perdue (signalee dans les logs).
 */
@Component
@Slf4j
public class VoyageClientFallback implements FallbackFactory<VoyageClient> {

    @Override
    public VoyageClient create(Throwable cause) {
        return (employeId, actif) -> log.warn("voyage-service indisponible, voyages de l'employe {} non synchronises : {}",
                employeId, cause.toString());
    }
}
