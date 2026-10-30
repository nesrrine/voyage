package com.voyagesaffaires.common.feign;

import com.voyagesaffaires.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Aide pour les fallbacks des clients OpenFeign proteges par le circuit breaker.
 * Une erreur metier du service appele (4xx : 403, 404...) est propagee telle quelle ;
 * une panne (service arrete, delai depasse, circuit ouvert) devient une 503 explicite.
 */
public final class FeignFallbacks {

    private FeignFallbacks() {
    }

    public static ApiException propager(Throwable cause, String service) {
        if (cause instanceof ApiException api && api.getStatus().is4xxClientError()) {
            return api;
        }
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Le " + service + " est momentanement indisponible");
    }

    public static boolean estPanne(Throwable cause) {
        return !(cause instanceof ApiException api && api.getStatus().is4xxClientError());
    }
}
