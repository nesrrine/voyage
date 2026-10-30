package com.voyagesaffaires.voyage.client;

import com.voyagesaffaires.common.dto.StatsDtos.UserStats;
import com.voyagesaffaires.common.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Appels vers le user-service (adresse trouvee dans Eureka, jeton JWT relaye). */
@FeignClient(name = "user-service", fallbackFactory = UserClientFallback.class)
public interface UserClient {

    @GetMapping("/api/users/{id}")
    UserDto utilisateur(@PathVariable("id") Long id);

    @GetMapping("/api/users/stats")
    UserStats statistiques();
}
