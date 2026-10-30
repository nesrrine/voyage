package com.voyagesaffaires.voyage.client;

import com.voyagesaffaires.common.dto.StatsDtos.ForumStats;
import com.voyagesaffaires.common.feign.FeignFallbacks;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/** Le tableau de bord reste disponible meme si le forum-service est arrete. */
@Component
@Slf4j
public class ForumClientFallback implements FallbackFactory<ForumClient> {

    @Override
    public ForumClient create(Throwable cause) {
        return () -> {
            if (!FeignFallbacks.estPanne(cause)) {
                throw FeignFallbacks.propager(cause, "service forum");
            }
            log.warn("forum-service indisponible : statistiques du forum remplacees par des zeros ({})", cause.toString());
            return ForumStats.vide();
        };
    }
}
