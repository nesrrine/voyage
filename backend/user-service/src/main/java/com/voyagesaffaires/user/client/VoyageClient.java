package com.voyagesaffaires.user.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Appel du voyage-service (adresse trouvee dans Eureka) : quand un employe est desactive,
 * ses voyages ne sont plus proposes par le matching.
 * (PUT et non PATCH : le client HTTP par defaut de Feign ne supporte pas PATCH.)
 */
@FeignClient(name = "voyage-service")
public interface VoyageClient {

    @PutMapping("/api/voyages/employes/{employeId}/actif")
    void majEmployeActif(@PathVariable("employeId") Long employeId, @RequestParam("actif") boolean actif);
}
