package com.voyagesaffaires.voyage.client;

import com.voyagesaffaires.common.dto.StatsDtos.ForumStats;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/** Appels vers le forum-service pour le tableau de bord de l'administrateur. */
@FeignClient(name = "forum-service", fallbackFactory = ForumClientFallback.class)
public interface ForumClient {

    @GetMapping("/api/forum/stats")
    ForumStats statistiques();
}
