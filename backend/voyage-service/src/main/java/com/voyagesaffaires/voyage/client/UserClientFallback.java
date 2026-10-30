package com.voyagesaffaires.voyage.client;

import com.voyagesaffaires.common.dto.StatsDtos.UserStats;
import com.voyagesaffaires.common.dto.UserDto;
import com.voyagesaffaires.common.feign.FeignFallbacks;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/** Reponse de repli quand le user-service est en panne ou que le circuit est ouvert. */
@Component
@Slf4j
public class UserClientFallback implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(Throwable cause) {
        return new UserClient() {
            @Override
            public UserDto utilisateur(Long id) {
                // Sans la fiche de l'employe, impossible de creer le voyage : on remonte l'erreur
                throw FeignFallbacks.propager(cause, "service utilisateurs");
            }

            @Override
            public UserStats statistiques() {
                if (!FeignFallbacks.estPanne(cause)) {
                    throw FeignFallbacks.propager(cause, "service utilisateurs");
                }
                log.warn("user-service indisponible : statistiques utilisateurs remplacees par des zeros ({})", cause.toString());
                return UserStats.vide();
            }
        };
    }
}
